# UFEed Java Bindings
-   [Introduction](#introduction)
-   [Getting started](#getting-started)
-   [Interface](#interface)
    -   [UFEMessage and UFEMessage Builder](#ufemessage-and-ufemessage-builder)
    -   [UFEedClient](#ufeedclient)
-   [Constants](#constants)
    -   [FIX variants constants](#fix-variants-constants)
-   [Building](#building)

------------------------------------------------------------------------

# Introduction

The UFEed Java Adapter (`UFEed_Java`) provides a low level java interface to the
UFEGW. Interactions with the UFEGW are based around a `UFEedClient`
object from `com.fix8mt.ufe.ufeedclient` namespace which can be used to
send and receive Messages to and from the UFEGW.

Use the following [Universal FIX engine documentaion](https://fix8mt.atlassian.net/wiki/spaces/FMT/pages/634438/Universal+FIX+Engine+Home) for a reference.

Features of `UFEedClient`:

-   System API support (see 4. Implementation Guide - Section 1.3)

-   Business API support (eg. NewOrderSingle and standard FIX messages)

-   Provides a 4-way communications API in order to make requests,
    publish messages, receive responses and subscribe to broadcast
    messages

-   User defined implementation of callback interface to handle these
    PUB, SUB, REQ and REP message events

-   Dynamic configuration of PUB, SUB, REQ, REP addressing and topics

-   Internal session management

Features of a `UFEMessage/UFEMessage.Builder`:

-   A generic container of fields, i.e. tag/typed value pairs with
    possible nested messages called groups

-   Smart field creation and access, rendering field value to ival, sval
    or fval depending on context

-   Named Message setters/getters (name, long_name, seq, service_id,
    subservice_id)

# Getting started

The `UFEed_Java` is provided as a Java project with jar
packages for external dependencies: ZeroMQ, Google Protobuf, getopt.
Within the target build directory of `UFEed_Java` we find the following directory
structure (example is Linux):

```
ufeed_bindings_java
├── packages
│   ├── java-getopt-1.0.14.jar
│   ├── jzmq-core-3.1.1-SNAPSHOT.jar
│   ├── jzmq-devices-3.1.1-SNAPSHOT.jar
│   ├── jzmq-jni-3.1.1-SNAPSHOT.jar
│   ├── jzmq-jni-3.1.1-SNAPSHOT-native-amd64-Linux.jar
│   ├── protobuf-java-3.7.1.jar
│   ├── protobuf-java-util-3.7.1.jar
│   └── ufeedclient-19.6.0.jar
└── samples
    └── com
        └── fix8mt
            └── ufe
                └── sample
                    └── Sample.java
```

# Interface

The main `UFEed_Java` interfaces/classes are `UFEMessage`, `UFEMEssage.Builder`
and `UFEedClient`. Most of `UFEMessage` field setters and `UFEedClient`
setters follow 'builder' pattern to simplify Java language constructs
(i.e. setters return the reference to an object it was called from):

```java
UFEMessage.Builder login = _uc.createMessage()
    .setLongName("login")
    .setType(st_system)
    .setServiceId(UFE_CMD_LOGIN)
    .addField(UFE_CMD, UFE_CMD_LOGIN, fl_system)
    .addField(UFE_LOGIN_ID, "webuser", fl_system)
    .addField(UFE_LOGIN_PW, "5e884898da28047151d0e56f8dc", fl_system);
```

## UFEMessage and UFEMessage Builder

The UFEMessage class provides some wrapping logic around the internal
Google Protobuf `WireMessage` format utilized by the UFEGW. Messages are
objects with which requests are made from the `UFEed_Java` to the UFEGW.

`UFEedClient::createMessage()` factory method shall be used to create a
message. `UFEMessage` is a final, i.e immutable just after
`UFEMessage::build()` is called to finalize `UFEMessage`.
`UFEMessage.Builder` class is for creating the message until it is
finalized.

`UFEMessage.Builder` class:

```java
public static class Builder {
    /**
    * ctors
    * @param wm WireMessage to copy from or null
    */
    public Builder(WireMessage wm);
    public Builder(WireMessage.Builder wmb);

    /**
    * Returns WireMessage builder
    * @return WireMessage builder
    */
    public WireMessage.Builder getWireMessageBuilder();

    /**
    * Longname
    * @return message long name
    */
    public String getLongName();
    public Builder setLongName(String longName);

    /**
    * Type
    * @return message type
    */
    public WireMessage.Type getType();
    public Builder setType(WireMessage.Type type);

    /**
    * Service id
    * @return message service id
    */
    int getServiceId();
    public Builder setServiceId(int serviceId);

    /**
    * Adds typed field to message
    * @param tag field tag
    * @param val field value
    * @param loc field location
    * @return self
    */
    public Builder addField(int tag, long val, UFEFieldLocation loc);
    public Builder addField(int tag, ByteString val, UFEFieldLocation loc);
    public Builder addField(int tag, String val, UFEFieldLocation loc);
    public Builder addField(int tag, char val, UFEFieldLocation loc);
    public Builder addField(int tag, double val, UFEFieldLocation loc);
    public Builder addField(int tag, boolean val, UFEFieldLocation loc);
    public Builder addField(int tag, Instant val, UFEFieldLocation loc);
    public Builder addField(int tag, UUID val, UFEFieldLocation loc);
    public Builder addField(int tag, Message.Status val, UFEFieldLocation loc);
    public Builder addField(int tag, Object val, UFEFieldLocation loc) throws UFEedException;
    public Builder addFields(Iterable<UFEField> fields);
    public Builder addGroup(int tag, GroupBuilderRef group, GroupTransformFunc tr, UFEFieldLocation loc);
    public Builder addGroupItem(UFEField.Builder group);
    
    /**
    * Builds UFEMessage when composing is complete. Message is ummutable agter the call.
    * @return Immutable composed UFEMessage
    */
    public UFEMessage build();
}
```

`UFEMessage` class:

```java
public class UFEMessage {
    /**
    * Creates a message builder. When it's done, call builder.build() that returns UFEMessage.
    * @param wm WireMessage to copy from or null
    * @return message builder
    */
    public static Builder newBuilder(WireMessage wm);

    /**
    * Returns inner WireMessage
    * @return inner WireMessage
    */
    public WireMessage getWireMessage();

    /**
     * Returns mapped fields hash map
     * @return mapped fields hash map
     */
    public HashMap<Integer, UFEField> getFields();

    /**
     * Returns mapped groups hash map
     * @return mapped groups hash map
     */
    public HashMap<Integer, List<UFEMessage>> getGroups();

    /**
     * Finds field by given tag
     * @param tag tag to find field
     * @return found field or null
     */
    public UFEField findField(int tag);

    /**
     * Finds field value by given tag
     * @param tag tag to find field
     * @return found field or null
     */
    public Object findFieldValue(int tag);

    /**
    * Finds group by given tag
    * @param tag tag to find group
    * @return found group of null
    */
    public List<UFEMessage> findGroup(int tag) {
        return _groups.get(tag);
    }
}
```

`UFEMessage` and `UFEMessage.Builder` usage sample:

```java
// logon
UFEMessage.Builder login = _uc.createMessage()
    .setLongName("login")
    .setType(st_system)
    .setServiceId(UFE_CMD_LOGIN)
    .addField(UFE_CMD, UFE_CMD_LOGIN, fl_system)
    .addField(UFE_LOGIN_ID, "webuser", fl_system)
    .addField(UFE_LOGIN_PW, "5e884898da28047151d0e56f8dc", fl_system);
// UFEedClient send methods accept builder class 
UFEMessage loginMsg = login.build();
UFEField loginId = loginMsg.findField(UFE_LOGIN_ID);
Object loginPw = loginMsg.findFieldValue(UFE_LOGIN_PW);
```

`UFEMessage` and `UFEMessage.Builder` create a `NewOrderSingle` message
with groups:

```java
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
    .addField(Side.tag, Side.BUY, fl_body)
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
    }, fl_body);
```

## UFEedClient

The `UFEedClient` class is used as the interface to make both System and
Business API calls to the UFEGW. Sessions between `UFEedClient` and the
UFEGW are made up of ZeroMQ PUB/SUB and REQ/REP sockets. The network
addresses and message topics inherent to these sockets are configurable
via `UFEedClient`. In addition, the `UFEedClient` manages these UFEGW
sessions on behalf of the user (after the user has successfully logged
in).

`UFEedClient` provides a callback interface called `Listener` that must
be implemented by `UFEedClient` consumer:

```java
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
```
:::
:::

`UFEedClient` is configured with `UFEConfiguration` class:

```java
public class UFEedConfiguration {
    /**
    * Subscriber endpoint, defaults to "tcp://127.0.0.1:55745"
    * @return subscriber endpoint
    */
    public String getSubscriber();
    public UFEedConfiguration setSubscriber(String subscriber);

    /**
    * Requester endpoint, defaults to "tcp://127.0.0.1:55746"
    * @return requester endpoint
    */
    public String getRequester();
    public UFEedConfiguration setRequester(String requester);

    /**
    * Publisher endpoint, defaults to "tcp://*:55747"
    * @return publisher endpoint
    */
    public String getPublisher();
    public UFEedConfiguration setPublisher(String publisher);

    /**
    * Responder endpoint, defaults to "tcp://*:55748"
    * @return responder endpoint
    */
    public String getResponder();
    public UFEedConfiguration setResponder(String responder);

    /**
    * Subscriber topic, defaults to "ufegw-publisher"
    * @return subscriber topic
    */
    public String getSubscriberTopic();
    public UFEedConfiguration setSubscriberTopic(String subscriberTopic);

    /**
    * Requester topic, defaults to "ufegw-responder"
    * @return requester topic
    */
    public String getRequesterTopic();
    public UFEedConfiguration setRequesterTopic(String requesterTopic);

    /**
    * Publisher topic, defaults to "ufeedclient-publisher"
    * @return publisher topic
    */
    public String getPublisherTopic();
    public UFEedConfiguration setPublisherTopic(String publisherTopic);

    /**
    * Responder topic, defaults to "ufeedclient-responder"
    * @return responder topic
    */
    public String getResponderTopic();
    public UFEedConfiguration setResponderTopic(String responderTopic);

    /**
    * ZMQ max IO threads
    * @return ZMQ max io threads
    */
    public int getMaxIoThreads();
    public UFEedConfiguration setMaxIoThreads(int maxIoThreads);

    /**
    * Poll interval in milliseconds
    * @return poll interval in milliseconds
    */
    public int getPollIntervalMs();
    public UFEedConfiguration setPollIntervalMs(int pollIntervalMs);
}
```

`UFEedClient` interface:

```java
public class UFEedClient implements AutoCloseable {
    /**
    * Constructs UFEedClient
    * @param configuration configuration to use
    */
    public UFEedClient(UFEedConfiguration configuration, Listener listener);

    /**
    * Implementation for AutoClosable::close() - frees ZMQ resources
    * @throws Exception
    */
    @Override
    public void close() throws Exception;

    /**
    * UFEedClient configuration
    * @return UFEedClient configuration
    */
    public UFEedConfiguration getConfiguration();

    /**
    * Starts UFEedClient. When started in synchronous mode (wait = true)
    * it does not return until stop() is called from a different thread.
    * @param wait true for synchronous call, false for asynchronous
    */
    public void start(boolean wait);

    /**
    * Stops UFEedClient
    * @throws InterruptedException
    */
    public void stop() throws InterruptedException;

    /**
    * Creates UFEMessage
    * @param longName message long name
    * @param type message type
    * @param serviceId message service id
    * @return message builder
    */
    public UFEMessage.Builder createMessage(String longName, WireMessage.Type type, int serviceId);
    public UFEMessage.Builder createMessage(WireMessage wm);

    /**
    * Synchronously sends request to UFE and waits for UFE response
    * @param request request to send
    * @return received response
    * @throws UFEedException thrown if no session token found
    * @throws InvalidProtocolBufferException thrown if protobuf parsing failed
    */
    public UFEMessage request(UFEMessage.Builder request) throws UFEedException, InvalidProtocolBufferException;

    /**
    * Send message to responder channel
    * IMPORTANT: Must be called from responderMessageReceived callback thread as much as possible
    * @param msg message to send
    */
    public void respond(UFEMessage msg);

    /**
    * UFEedClient callback interface
    */
    public interface Listener { ... }
}
```

`UFEedClient` usage sample:

```java
UFEedClient _uc = new UFEedClient(new UFEedConfiguration().setSubscriber(SUBSCRIBER_DEFAULT).setResponderTopic("ufegw-requester"),
  new UFEedClient.Listener() {
    @Override
    public void subscriptionMessageReceived(UFEMessage message) { ... }
    @Override
    public void responderMessageReceived(UFEMessage message) { ... }
    @Override
    public void responseMessageReceived(UFEMessage message) { ... }
    @Override
    public boolean authenticateRequested(String user, String password) { ... }
    @Override
    public boolean zeroMQErrorHappened(int error) { ... }
    @Override
    public boolean errorHappened(String error, Exception exception) { ... }
});
_uc.start(false);

// logon
UFEMessage.Builder login = _uc.createMessage("login", st_system, UFE_CMD_LOGIN)
    .addField(UFE_CMD, UFE_CMD_LOGIN, fl_system)
    .addField(UFE_LOGIN_ID, "webuser", fl_system)
    .addField(UFE_LOGIN_PW, "5e884898da28047151d0e56f8dc", fl_system);
try {
    UFEMessage response = _uc.request(login);
    if (response.findField(UFE_SESSION_TOKEN) == null)
        throw new UFEedException("Session token is missing");
    Object sessToken = response.findFieldValue(UFE_SESSION_TOKEN);
    if (sessToken == null)
        throw new UFEedException("Session token is missing #1");
    if(!(sessToken instanceof UUID))
        throw new UFEedException("Session token is of unexpected type");
    if (sessToken.toString().isEmpty())
        throw new UFEedException("Session token is empty");

    // service list request
    response = _uc.request(_uc
        .createMessage("service_list", st_system, UFE_CMD_SERVICE_LIST)
        .addField(UFE_CMD, UFE_CMD_SERVICE_LIST, fl_system));
...
} finally {
    _uc.stop();
    _uc.close();
}
```

# Constants

The `UFEed_Java` maintains a list of constant values that translate to integer
codes in the UFEGW. These integer codes are used to identify System API
services as well as general FIX functionality. A full list of these
constants is available at `src/com/fix8mt/ufe/ufeedclient/Consts.java`.
The file with constants could be regenrated using `genconsts` project.

## FIX variants constants

The `UFEed_Java` provides constants for all stock FIX variants:

```java
import com.fix8mt.ufe.FIX50SP2.ufe_java_fields_fix50sp2.*;
...
UFEMessage.Builder nos = _uc.createMessage()
    .setLongName("NewOrderSingle")
    .setType(st_fixmsg)
    .setServiceId(1)
    .setName(MsgType.NEWORDERSINGLE)
    .addField(ClOrdID.tag, "123", fl_body)
    .addField(TransactTime.tag, Instant.now(), fl_body)
    .addField(ExecInst.tag, ExecInst.ALL_OR_NONE, fl_body)
    .addField(OrdType.tag, OrdType.LIMIT, fl_body)
    .addField(Side.tag, Side.BUY, fl_body);
```

# Building

The `UFEed_Java` build follows a standard Java build pattern. Jetbrains IntelliJ project files are provided.

The `UFEed_Java` provides a sample to use as a starting point for UFEed Java
development. The sample is under root folder. To build/run sample, you
have to set Java path to `UFEedClient` jar and all the dependency jars
and call `Sample.Main()`:
