package com.fix8mt.ufe.ufeedclient;

import static com.fix8mt.ufe.ufeedclient.Consts.*;

/**
 * UFEedClient configuration class
 * {@code
 *  try(UFEedClient uc = new UFEedClient(new UFEedConfiguration()
 *		.setSubscriber(SUBSCRIBER_DEFAULT)
 *		.setPublisher("tcp://*:56747"))
 *		)
 *  {
 *  	...
 *  }
 * }
 */
public class UFEedConfiguration {
	private String _subscriber = SUBSCRIBER_DEFAULT;
	private String _requester = REQUESTER_DEFAULT;
	private String _publisher = PUBLISHER_DEFAULT;
	private String _responder = RESPONDER_DEFAULT;
	private String _subscriberTopic = SUBSCRIBER_TOPIC_DEFAULT;
	private String _requesterTopic = REQUESTER_TOPIC_DEFAULT;
	private String _publisherTopic = PUBLISHER_TOPIC_DEFAULT;
	private String _responderTopic = RESPONDER_TOPIC_DEFAULT;
	private int _maxIoThreads = 1;
	private int _pollIntervalMs = 10;

	/**
	 * Subscriber endpoint, defaults to "tcp://127.0.0.1:55745"
	 * @return subscriber endpoint
	 */
	public String getSubscriber() {
		return _subscriber;
	}

	/** Sets subscriber endpoint
	 * @param subscriber subscriber endpoint address
	 * @return self
	 */
	public UFEedConfiguration setSubscriber(String subscriber) {
		_subscriber = subscriber;
		return this;
	}

	/**
	 * Requester endpoint, defaults to "tcp://127.0.0.1:55746"
	 * @return requester endpoint
	 */
	public String getRequester() {
		return _requester;
	}

	/** Sets requester endpoint
	 * @param requester requester endpoint address
	 * @return self
	 * */
	public UFEedConfiguration setRequester(String requester) {
		_requester = requester;
		return this;
	}

	/**
	 * Publisher endpoint, defaults to "tcp://*:55747"
	 * @return publisher endpoint
	 */
	public String getPublisher() {
		return _publisher;
	}

	/** Sets publisher endpoint
	 * @param publisher publisher endpoint address
	 * @return self
	 */
	public UFEedConfiguration setPublisher(String publisher) {
		_publisher = publisher;
		return this;
	}

	/**
	 * Responder endpoint, defaults to "tcp://*:55748"
	 * @return responder endpoint
	 */
	public String getResponder() {
		return _responder;
	}

	/**
	 * Sets responder endpoint
	 * @param responder responder endpoint address
	 * @return self
	 */
	public UFEedConfiguration setResponder(String responder) {
		_responder = responder;
		return this;
	}

	/**
	 * Subscriber topic, defaults to "ufegw-publisher"
	 * @return subscriber topic
	 */
	public String getSubscriberTopic() {
		return _subscriberTopic;
	}

	/**
	 * Sets subscriber topic
	 * @param subscriberTopic subscriber topic string
	 * @return self
	 */
	public UFEedConfiguration setSubscriberTopic(String subscriberTopic) {
		_subscriberTopic = subscriberTopic;
		return this;
	}

	/**
	 * Requester topic, defaults to "ufegw-responder"
	 * @return requester topic
	 */
	public String getRequesterTopic() {
		return _requesterTopic;
	}

	/**
	 * Sets requester topic, defaults to "ufegw-responder"
	 * @param requesterTopic requester topic string
	 * @return self
	 */
	public UFEedConfiguration setRequesterTopic(String requesterTopic) {
		_requesterTopic = requesterTopic;
		return this;
	}

	/**
	 * Publisher topic, defaults to "ufeedclient-publisher"
	 * @return publisher topic
	 */
	public String getPublisherTopic() {
		return _publisherTopic;
	}

	/**
	 * Sets publisher topic, defaults to "ufeedclient-publisher"
	 * @param publisherTopic publisher topic string
	 * @return self
	 */
	public UFEedConfiguration setPublisherTopic(String publisherTopic) {
		_publisherTopic = publisherTopic;
		return this;
	}

	/**
	 * Responder topic, defaults to "ufeedclient-responder"
	 * @return responder topic
	 */
	public String getResponderTopic() {
		return _responderTopic;
	}

	/**
	 * Sets responder topic, defaults to "ufeedclient-responder"
	 * @param responderTopic responder topic string
	 * @return self
	 */
	public UFEedConfiguration setResponderTopic(String responderTopic) {
		_responderTopic = responderTopic;
		return this;
	}

	/**
	 * Gets ZMQ max IO threads
	 * @return ZMQ max io threads
	 */
	public int getMaxIoThreads() {
		return _maxIoThreads;
	}

	/**
	 * Sets max ZMQ IO threads
	 * @param maxIoThreads max ZMQ IO threads
	 * @return self
	 */
	public UFEedConfiguration setMaxIoThreads(int maxIoThreads) {
		_maxIoThreads = maxIoThreads;
		return this;
	}

	/**
	 * Gets poll interval in milliseconds
	 * @return poll interval in milliseconds
	 */
	public int getPollIntervalMs() {
		return _pollIntervalMs;
	}

	/**
	 * Sets poll interval in milliseconds
	 * @param pollIntervalMs poll interval in milliseconds
	 * @return self
	 */
	public UFEedConfiguration setPollIntervalMs(int pollIntervalMs) {
		_pollIntervalMs = pollIntervalMs;
		return this;
	}
}
