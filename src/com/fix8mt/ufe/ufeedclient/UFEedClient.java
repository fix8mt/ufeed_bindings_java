package com.fix8mt.ufe.ufeedclient;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.nio.charset.Charset;
import java.util.UUID;

import static com.fix8mt.ufe.Ufeapi.UFEField;
import static com.fix8mt.ufe.Ufeapi.UFEField.UFEFieldLocation.fl_body;
import static com.fix8mt.ufe.Ufeapi.UFEField.UFEFieldLocation.fl_system;
import static com.fix8mt.ufe.Ufeapi.WireMessage;
import static com.fix8mt.ufe.Ufeapi.WireMessage.Type.*;
import static com.fix8mt.ufe.ufeedclient.Consts.*;

/**
 * A class to communicate to UFE
 * {@code
 * }
 */
public class UFEedClient implements AutoCloseable {
	private UFEedConfiguration _cs;
	private ZContext _context;
	private ZMQ.Socket _reqSocket;
	private ZMQ.Socket _repSocket;
	private ByteString _sessionId = ByteString.EMPTY;
	private boolean _started = false;
	private int _responderSeq = 0;
	private Thread _workerThread = null;
	private boolean _stopRequested = false;
	private Listener _listener;
	private final Object _reqSocketLock = new Object();
	private final Object _repSocketLock = new Object();

	/**
	 * Constructs UFEedClient
	 * @param configuration configuration to use
	 */
	public UFEedClient(UFEedConfiguration configuration, Listener listener) {
		_cs = configuration;
		_context = new ZContext();
		_context.setIoThreads(configuration.getMaxIoThreads());
		_reqSocket = _context.createSocket(ZMQ.REQ);
		_listener = listener;
	}

	/**
	 * Implementation for AutoClosable::close() - frees ZMQ resources
	 * @throws Exception
	 */
	@Override
	public void close() throws Exception {
		stop();
		if (!_sessionId.isEmpty())
		{
			request(createMessage()
				.setLongName("logout")
				.setType(st_system)
				.setServiceId(UFE_CMD_LOGOUT)
				.addField(UFE_CMD, UFE_CMD_LOGOUT, fl_body));
			_sessionId = ByteString.EMPTY;
		}
		_reqSocket.close();
	}

	/**
	 * UFEedClient configuration
	 * @return UFEedClient configuration
	 */
	public UFEedConfiguration getConfiguration() {
		return _cs;
	}

	/**
	 * Starts UFEedClient. When started in synchronous mode (wait = true)
	 * it does not return until stop() is called from a different thread.
	 * @param wait true for synchronous call, false for asynchronous
	 */
	public void start(boolean wait) {
		if (_started)
			return;
		_started = true;
		_reqSocket.connect(_cs.getRequester());
		if (wait) {
			worker();
		} else {
			_workerThread = new Thread(this::worker);
			_workerThread.start();
		}
	}

	/**
	 * Stops UFEedClient
	 * @throws InterruptedException
	 */
	public void stop() throws InterruptedException {
		if (!_started)
			return;
		_stopRequested = true;
		if (_workerThread != null)
			_workerThread.join();
	}

	/**
	 * Creates UFEMessage
	 * @return message builder
	 */
	public UFEMessage.Builder createMessage() {
		return UFEMessage.newBuilder(null);

	}

	/**
	 * Creates UFEMessage
	 * @param wm WireMessage to copy from
	 * @return message builder
	 */
	public UFEMessage.Builder createMessage(WireMessage wm) {
		return UFEMessage.newBuilder(wm);

	}

	/**
	 * Synchronously sends request to UFE and waits for UFE response
	 * @param request request to send
	 * @return received response
	 * @throws UFEedException thrown if no session token found
	 * @throws InvalidProtocolBufferException thrown if protobuf parsing failed
	 */
	public UFEMessage request(UFEMessage.Builder request) throws UFEedException, InvalidProtocolBufferException {
		WireMessage wm;
		// have we already logged in?
		if (!_sessionId.isEmpty()) {
			request.addField(UFE_SESSION_TOKEN, _sessionId, fl_system);
			wm = sendRequestRecvResponse(request);
		}
		// well ok, is this a login attempt?
		else if (request.getServiceId() == UFE_CMD_LOGIN) {
			wm = sendRequestRecvResponse(request);
			if (wm != null) {
				wm.getFieldsList().stream()
					.filter(ufeField -> ufeField.getTag() == UFE_SESSION_TOKEN).findFirst()
					.ifPresent(sessionField -> _sessionId = sessionField.getSval());
			}
		}
		// neither?
		else
			throw new UFEedException("No session token found - you must log on before making a request");

		// send REP WireMessage to handler function
		UFEMessage response = createMessage(wm).build();
		_listener.responseMessageReceived(response);
		return response;

	}

	/**
	 * Send message to responder channel
	 * IMPORTANT: Must be called from responderMessageReceived callback thread as much as possible
	 * @param msg message to send
	 */
	public void respond(UFEMessage msg) {
		synchronized (_repSocketLock) {
			_repSocket.sendMore(_cs.getResponderTopic());
			_repSocket.send(msg.getWireMessage().toByteArray(), 0);
		}
	}

	/**
	 * UFEedClient callback interface
	 */
	public interface Listener {
		/**
		 * Called when subscription message received
		 * @param message received subscription message
		 */
		void subscriptionMessageReceived(UFEMessage message);

		/**
		 * Called when responder message received
		 * @param message received responder message
		 */
		void responderMessageReceived(UFEMessage message);

		/**
		 * Called when response message received
		 * @param message received response message
		 */
		void responseMessageReceived(UFEMessage message);

		/**
		 * Called when authentication is requested
		 * @param user user to check
		 * @param password user password to check
		 * @return true for successful authentication, otherwise false
		 */
		boolean authenticateRequested(String user, String password);

		/**
		 * Called when ZeroMQ error happened
		 * @param error ZMQ error code
		 * @return true to continue, false to stop processing loop
		 */
		boolean zeroMQErrorHappened(int error);

		/**
		 * Called when error happened
		 * @param error error message
		 * @param exception exception happened
		 * @return true to continue, false to stop processing loop
		 */
		boolean errorHappened(String error, Exception exception);
	}

	private WireMessage sendRequestRecvResponse(UFEMessage.Builder msg) throws InvalidProtocolBufferException {
		byte[] msgBytes;
		synchronized (_reqSocketLock) {
			_reqSocket.sendMore(_cs.getRequesterTopic());
			_reqSocket.send(msg.build().getWireMessage().toByteArray(), 0);
			_reqSocket.recvStr(0, Charset.defaultCharset());
			msgBytes = _reqSocket.recv(0);
		}
		return WireMessage.parseFrom(msgBytes);
	}

	private void worker() {
		_repSocket = null;
		try (ZMQ.Socket subSocket = _context.createSocket(ZMQ.SUB)) {
			subSocket.connect(_cs.getSubscriber());
			subSocket.subscribe(_cs.getSubscriberTopic().getBytes());

			_repSocket = _context.createSocket(ZMQ.REP);
			if (!_cs.getResponder().isEmpty())
				_repSocket.bind(_cs.getResponder());

			ZMQ.Poller poller = new ZMQ.Poller(2);
			poller.register(subSocket, ZMQ.Poller.POLLIN);
			poller.register(_repSocket, ZMQ.Poller.POLLIN);
			while (!_stopRequested && !Thread.currentThread().isInterrupted()) {
				try {
					if (poller.poll(_cs.getPollIntervalMs()) < 0)
						break; // interrupted
					if (poller.pollin(0)) {
						// subscriber message
						subSocket.recvStr(0, Charset.defaultCharset());
						UFEMessage um = createMessage(WireMessage.parseFrom(subSocket.recv(0))).build();
						_listener.subscriptionMessageReceived(um);
					}
					if (poller.pollin(1)) {
						// responder message
						byte[] msgBytes;
						synchronized (_repSocketLock) {
							_repSocket.recvStr(0, Charset.defaultCharset());
							msgBytes = _repSocket.recv(0);
						}
						UFEMessage um = createMessage(WireMessage.parseFrom(msgBytes)).build();
						_listener.responderMessageReceived(um);
						processRespondMessage(um);
					}
				} catch (ZMQException e) {
					if (e.getErrorCode() == ZMQ.Error.EAGAIN.getCode())
						continue;
					_stopRequested = !_listener.zeroMQErrorHappened(e.getErrorCode());
				} catch (Exception ex) {
					_stopRequested = !_listener.errorHappened(ex.getMessage(), ex);
				}
			}
		} finally {
			if (_repSocket != null) {
				_repSocket.close();
			}
		}
	}

	private UFEMessage processRespondMessage(UFEMessage msg) {
		UFEMessage.Builder rum = UFEMessage.newBuilder(null);
		rum.getWireMessageBuilder()
			.setServiceId(msg.getWireMessage().getServiceId())
			.setSubserviceId(msg.getWireMessage().getSubserviceId());

		ByteString reqToken = null;
		long responseCode = UFE_OK;
		String responseText = null;
		UFEField rToken = msg.findField(UFE_REQUEST_TOKEN);
		if (rToken != null)
			reqToken = rToken.getSval();
		switch (msg.getWireMessage().getType()) {
			case st_fixmsg:
				break;
			case st_system:
				UFEField cmd = msg.findField(UFE_CMD);
				if (cmd == null) {
					responseText =  String.format("command not present %s on topic=%s", msg.getWireMessage().getType(), _cs.getResponderTopic());
					responseCode = NO_CMD;
				} else {
					long cmdToken = cmd.getIval();
					if ((int) cmdToken == UFE_CMD_AUTHENTICATE) { // authentication response
						UFEField usrToken = msg.findField(553); // Username
						String userName = usrToken == null ? "" : usrToken.getSval().toString();
						UFEField pwToken = msg.findField(554); // Password
						String password = pwToken == null ? "" : pwToken.getSval().toString();
						boolean accepted = _listener.authenticateRequested(userName, password);
						if (accepted) {
							responseText = "authentication was successful";
							responseCode = LOGIN_ACCEPTED;
						} else {
							responseText = "user or password is incorrect";
							responseCode = UNKNOWN_USER;
						}
					} else {
						responseCode = NO_CMD;
					}
					rum.addField(UFE_CMD_RESPONSE, cmdToken, fl_system);
				}
				break;
			default:
				responseText = String.format("unknown message type or command %s  on topic=%s", msg.getWireMessage().getType(), _cs.getResponderTopic());
				responseCode = UNKNOWN_TYPE;
				break;
		}
		// Generate response and send
		int seq = ++_responderSeq;
		rum.getWireMessageBuilder().setSeq(seq);
		rum.addField(UFE_RESPONSE_CODE, new UFEMessage.Status(responseCode), fl_system);
		rum.addField(COMMON_REFSEQNUM, seq, fl_system);
		if (reqToken != null && !reqToken.isEmpty())	// if request token received, echo back
			rum.addField(UFE_REQUEST_TOKEN, reqToken, fl_system);
		switch ((int) responseCode) {
			case UFE_OK:
			case LOGIN_ACCEPTED:
				rum.addField(UFE_RESPONSE_TOKEN, UUID.randomUUID(), fl_system);
				rum.getWireMessageBuilder().setType(st_response);
				break;
			default: // is an error
				rum.getWireMessageBuilder().setType(st_error);
				break;
		}

		if (responseText!= null && !responseText.isEmpty()) // both errors and success can pass a text msg
			rum.addField(COMMON_TEXT, responseText, fl_system);

		UFEMessage rumm = rum.build();
		respond(rumm);
		return rumm;
	}
}
