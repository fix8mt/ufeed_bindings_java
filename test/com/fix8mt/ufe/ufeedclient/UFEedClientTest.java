package com.fix8mt.ufe.ufeedclient;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.fix8mt.ufe.Ufeapi.UFEField.UFEFieldLocation.fl_body;
import static com.fix8mt.ufe.Ufeapi.UFEField.UFEFieldLocation.fl_system;
import static com.fix8mt.ufe.Ufeapi.WireMessage.Type.st_fixmsg;
import static com.fix8mt.ufe.Ufeapi.WireMessage.Type.st_system;
import static com.fix8mt.ufe.ufeedclient.Consts.*;
import com.fix8mt.ufe.FIX50SP2.ufe_java_fields_fix50sp2.*;
import static org.junit.jupiter.api.Assertions.*;

class UFEedClientTest {
	private UFEedClient _uc;
	private List<UFEMessage> _receivedSubMsgs = new ArrayList<>();
	private List<UFEMessage> _receivedResMsgs = new ArrayList<>();
	private List<UFEMessage> _receivedRepMsgs = new ArrayList<>();
	private final Object _lockAuth = new Object();
	private final Object _lockSub = new Object();
	private String _login, _password;
	private boolean _authenticated;

	@BeforeEach
	void setUp() {
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
					fail(String.format("ZeroMQ error happened %d", error));
					return false;
				}

				@Override
				public boolean errorHappened(String error, Exception exception) {
					fail(String.format("ZeroMQ error happened %s", error));
					return false;
				}
		});
	}

	@AfterEach
	void tearDown() {
		try {
			_uc.close();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	@DisplayName("UFEedClient test Logon/Auth")
	public void testLogonAuth() {
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
			assertTrue(_authenticated);
		}
		System.out.printf("authenticated\n");
	}

	@Test
	@DisplayName("UFEedClient test Logon/no Auth")
	public void testLogonNoAuth() {
		_uc.getConfiguration().setResponder("");
		testLogon();
	}

	@Test
	@DisplayName("NewOrderSingle message test")
	public void testMessage() {
		UFEMessage.Builder.GroupBuilderRef grp = new UFEMessage.Builder.GroupBuilderRef();
		UFEMessage.Builder nos = _uc.createMessage()
			.setLongName("NewOrderSingle")
			.setType(st_fixmsg)
			.setServiceId(1)
			.setName(MsgType.NEWORDERSINGLE)
			.addField(ClOrdID.tag, "123", fl_body)
			.addField(TransactTime.tag, Instant.now(), fl_body)
			.addField(ExecInst.tag, ExecInst.ALL_OR_NONE, fl_body)
			.addField(OrdType.tag, OrdType.LIMIT, fl_body)
			.addField(Price.tag, 123.456, fl_body, 4)
			.addField(OrderQty.tag, 456.789, fl_body, 2)
			.addField(Side.tag, Side.BUY, fl_body)
			.addGroup(NoAllocs.tag, grp, (builder, group) -> {
				UFEMessage.Builder.GroupBuilderRef g1 = new UFEMessage.Builder.GroupBuilderRef();
				builder.addGroupItem(group)
					.setLongName("NoAlloc")
					.setType(st_fixmsg)
					.setSeq(1)
					.addField(AllocAccount.tag, "ABC", fl_body)
					.addField(AllocQty.tag, 2, fl_body)
					.addGroup(NoPartyIDs.tag, g1, (builder1, group1) -> {
						builder1.addGroupItem(group1)
							.setLongName("NoPartyIDs")
							.setType(st_fixmsg)
							.setSeq(1)
							.addField(PartyID.tag, "sgo", fl_body);
					}, fl_body);
				builder.addGroupItem(group)
					.setLongName("NoAlloc")
					.setType(st_fixmsg)
					.setSeq(2)
					.addField(AllocAccount.tag, "CDE", fl_body)
					.addField(AllocQty.tag, 4, fl_body)
					.addGroup(NoPartyIDs.tag, g1, (builder1, group1) -> {
						builder1.addGroupItem(group1)
							.setLongName("NoPartyIDs")
							.setType(st_fixmsg)
							.setSeq(1)
							.addField(PartyID.tag, "ssv", fl_body);
					}, fl_body);
			}, fl_body)
			.addField(UFE_RESPONSE_CODE, new UFEMessage.Status(UFE_OK), fl_system)
			.addField(UFE_SESSION_TOKEN, UUID.randomUUID(), fl_system)
			;
		String s1 = nos.print();
		String s2 = nos.build().print();
		assertEquals(s1, s2);
	}

	private void testLogon() {
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
			assertEquals(1, _receivedResMsgs.size());
			assertNotNull(_receivedResMsgs.get(0).findField(UFE_SESSION_TOKEN));
			Object sessToken = _receivedResMsgs.get(0).findFieldValue(UFE_SESSION_TOKEN);
			assertNotNull(sessToken);
			assertTrue(sessToken instanceof UUID);
			assertNotEquals(0, sessToken.toString().length());
			assertEquals(response.findFieldValue(UFE_SESSION_TOKEN), sessToken);

			// service list request
			response = _uc.request(_uc
				.createMessage()
				.setLongName("service_list")
				.setType(st_system)
				.setServiceId(UFE_CMD_SERVICE_LIST)
				.addField(UFE_CMD, UFE_CMD_SERVICE_LIST, fl_system));
			assertEquals(1, response.getGroups().size());

			// subscription check
			synchronized(_lockSub)
			{
				_lockSub.wait(10000);
				assertTrue(_receivedSubMsgs.size() > 0);
			}
		} catch (UFEedException | InterruptedException | InvalidProtocolBufferException e) {
			e.printStackTrace();
			fail();
		}

	}
}