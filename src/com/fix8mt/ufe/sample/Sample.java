package com.fix8mt.ufe.sample;

import com.fix8mt.ufe.ufeedclient.UFEMessage;
import com.fix8mt.ufe.ufeedclient.UFEedClient;
import com.fix8mt.ufe.ufeedclient.UFEedException;
import com.fix8mt.ufe.ufeedclient.UFEedConfiguration;
import com.google.protobuf.InvalidProtocolBufferException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.fix8mt.ufe.FIX50SP2.ufe_java_fields_fix50sp2.*;
import static com.fix8mt.ufe.Ufeapi.UFEField.UFEFieldLocation.fl_body;
import static com.fix8mt.ufe.Ufeapi.UFEField.UFEFieldLocation.fl_system;
import static com.fix8mt.ufe.Ufeapi.WireMessage.Type.st_fixmsg;
import static com.fix8mt.ufe.Ufeapi.WireMessage.Type.st_system;
import static com.fix8mt.ufe.ufeedclient.Consts.*;

public class Sample implements AutoCloseable {

	private UFEedClient _uc;
	private List<UFEMessage> _receivedSubMsgs = new ArrayList<>();
	private List<UFEMessage> _receivedResMsgs = new ArrayList<>();
	private List<UFEMessage> _receivedRepMsgs = new ArrayList<>();
	private final Object _lockAuth = new Object();
	private final Object _lockSub = new Object();
	private String _login, _password;
	private boolean _authenticated;

	public Sample() {
		_uc = new UFEedClient(new UFEedConfiguration().setSubscriber(SUBSCRIBER_DEFAULT).setResponderTopic("ufegw-requester"),
			new UFEedClient.Listener() {
				@Override
				public void subscriptionMessageReceived(UFEMessage message) {
					synchronized (_lockSub) {
						_receivedSubMsgs.add(message);
						_lockSub.notify();
					}
				}

				@Override
				public void responderMessageReceived(UFEMessage message) {
					_receivedRepMsgs.add(message);
				}

				@Override
				public void responseMessageReceived(UFEMessage message) {
					_receivedResMsgs.add(message);
				}

				@Override
				public boolean authenticateRequested(String user, String password) {
					synchronized(_lockAuth)
					{
						_login = user;
						_password = password;
						_authenticated = true;
						_lockAuth.notify();
					}
					// accept any args.User/args.Password
					return true;
				}

				@Override
				public boolean zeroMQErrorHappened(int error) {
					System.err.printf("ZeroMQ error happened %d", error);
					return false;
				}

				@Override
				public boolean errorHappened(String error, Exception exception) {
					System.err.printf("ZeroMQ error happened %s", error);
					return false;
				}
			});
	}


	@Override
	public void close() throws Exception {
		try {
			_uc.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testLogonAuth() throws UFEedException {
		_uc.getConfiguration().setResponder(RESPONDER_DEFAULT);
		testLogon();
		// authentication check
		// - this requires manual run of f8ptest initiator to connect to one of the auth session in ufegw
		System.out.printf("waiting for auth...\n");
		synchronized(_lockAuth)
		{
			try {
				_lockAuth.wait(600*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(!_authenticated)
				throw new UFEedException("Not authenticated");
		}
		System.out.printf("authenticated\n");
	}

	private void testLogon() throws UFEedException {
		_uc.start(false);

		// logon
		UFEMessage.Builder login = _uc.createMessage()
			.setLongName("login")
			.setType(st_system)
			.setServiceId(UFE_CMD_LOGIN)
			.addField(UFE_CMD, UFE_CMD_LOGIN, fl_system)
			.addField(UFE_LOGIN_ID, "webuser", fl_system)
			.addField(UFE_LOGIN_PW, "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8", fl_system);
		try {
			UFEMessage response = _uc.request(login);
			if (1 != _receivedResMsgs.size())
				throw new UFEedException("No received REQ/REP messages available");

			if (_receivedResMsgs.get(0).findField(UFE_SESSION_TOKEN) == null)
				throw new UFEedException("Session token is missing");
			Object sessToken = _receivedResMsgs.get(0).findFieldValue(UFE_SESSION_TOKEN);
			if (sessToken == null)
				throw new UFEedException("Session token is missing #1");
			if(!(sessToken instanceof UUID))
				throw new UFEedException("Session token is of unexpected type");
			if (sessToken.toString().isEmpty())
				throw new UFEedException("Session token is empty");
			if(!response.findFieldValue(UFE_SESSION_TOKEN).toString().equals(sessToken.toString()))
				throw new UFEedException("Session token mismatch");

			// service list request
			UFEMessage.Builder.GroupBuilderRef grp = new UFEMessage.Builder.GroupBuilderRef();
			response = _uc.request(_uc
				.createMessage()
				.setLongName("service_list")
				.setType(st_system)
				.setServiceId(UFE_CMD_SERVICE_LIST)
				.addField(UFE_CMD, UFE_CMD_SERVICE_LIST, fl_system)
				.addGroup(NoAllocs.tag, grp, (builder, group) -> {
					builder.addGroupItem(group)
						.setLongName("NoAlloc")
						.setType(st_fixmsg)
						.setSeq(1)
						.addField(AllocAccount.tag, "ABC", fl_body)
						.addField(AllocQty.tag, 2, fl_body);
					builder.addGroupItem(group)
						.setLongName("NoAlloc")
						.setType(st_fixmsg)
						.setSeq(2)
						.addField(AllocAccount.tag, "CDE", fl_body)
						.addField(AllocQty.tag, 4, fl_body);
				}, fl_body)
			);
			if(1 != response.getGroups().size())
				throw new UFEedException("No response messages available");

			// subscription check
			synchronized(_lockSub)
			{
				_lockSub.wait(60*1000);
				if(_receivedSubMsgs.size() <= 0)
					throw new UFEedException("No received SUB messages available");
			}
			_uc.stop();

		} catch (UFEedException | InterruptedException | InvalidProtocolBufferException e) {
			e.printStackTrace();
			throw new UFEedException("Exception thrown");
		}
	}

	public void testNOS() {
		UFEMessage.Builder nos = _uc.createMessage()
			.setLongName("NewOrderSingle")
			.setType(st_fixmsg)
			.setServiceId(1)
			.setName(MsgType.NEWORDERSINGLE)
			.addField(ClOrdID.tag, "123", fl_body)
			.addField(TransactTime.tag, Instant.now(), fl_body)
			.addField(ExecInst.tag, ExecInst.ALL_OR_NONE, fl_body)
			.addField(OrdType.tag, OrdType.LIMIT, fl_body)
			.addField(Side.tag, Side.BUY, fl_body)
			;
	}

	public static void main(String[] args) {
		try(Sample sample = new Sample()) {
			sample.testLogonAuth();
			sample.testNOS();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}