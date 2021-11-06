package com.fix8mt.ufe.ufeedclient;

import com.fix8mt.ufe.Ufeapi.UFEField.UFEFieldLocation;
import com.google.protobuf.ByteString;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.fix8mt.ufe.Ufeapi.UFEField;
import static com.fix8mt.ufe.Ufeapi.UFEField.UFEFieldType.*;
import static com.fix8mt.ufe.Ufeapi.WireMessage;

/**
 * UFE message wrapper class
 */
public class UFEMessage {
	private WireMessage _wm;
	private HashMap<Integer, UFEField> _fields = new HashMap<>();
	private HashMap<Integer, List<UFEMessage>> _groups = new HashMap<>();

	/**
	 * Status class to keep long value of numeric status
	 */
	public static class Status {
		private long _status;
		public Status(long status) {
			_status = status;
		}
		public long status() {
			return _status;
		}
	}

	/**
	 * Builder class for building UFEMessage.
	 * It's required since WireMessage is immutable once created. When building is done,
	 * build() shall be called to complete UFEMEssage creation.
	 * {@code
	 *	UFEMessage.Builder.GroupBuilderRef grp = new UFEMessage.Builder.GroupBuilderRef();
	 *	UFEMessage.Builder nos = _uc.createMessage()
	 *		.setLongName("NewOrderSingle")
	 *		.setType(st_fixmsg)
	 *		.setServiceId(1)
	 *		.setName(MsgType.NEWORDERSINGLE)
	 *		.addField(ClOrdID.tag, "123", fl_body)
	 *		.addField(TransactTime.tag, Instant.now(), fl_body)
	 *		.addField(ExecInst.tag, ExecInst.ALL_OR_NONE, fl_body)
	 *		.addField(OrdType.tag, OrdType.LIMIT, fl_body)
	 *		.addField(Side.tag, Side.BUY, fl_body)
	 *		.addGroup(NoAllocs.tag, grp, (builder, group) -> {
	 *			builder.addGroupItem(group)
	 *				.setLongName("NoAlloc")
	 *				.setType(st_fixmsg)
	 *				.setSeq(1)
	 *				.addField(AllocAccount.tag, "ABC", fl_body)
	 *				.addField(AllocQty.tag, 2, fl_body);
	 *			builder.addGroupItem(group)
	 *				.setLongName("NoAlloc")
	 *				.setType(st_fixmsg)
	 *				.setSeq(2)
	 *				.addField(AllocAccount.tag, "CDE", fl_body)
	 *				.addField(AllocQty.tag, 4, fl_body);
	 *		}, fl_body)
	 * }
	 */
	public static class Builder {
		private WireMessage.Builder _builder;

		/**
		 * Constructs builder from WireMessage
		 * @param wm WireMessage to copy from or null
		 */
		public Builder(WireMessage wm) {
			_builder = (wm == null ? WireMessage.newBuilder() : wm.toBuilder());
		}

		/**
		 * Constructs builder from WireMessage.Builder
		 * @param wmb WireMessageBuilder to take ownership from
		 */
		public Builder(WireMessage.Builder wmb) {
			_builder = wmb;
		}

		/**
		 * Returns WireMessage builder
		 * @return WireMessage builder
		 */
		public WireMessage.Builder getWireMessageBuilder() {
			return _builder;
		}

		/**
		 * Longname getter
		 * @return message long name
		 */
		public String getLongName() {
			return _builder.getLongname();
		}

		/**
		 * Sets message long name
		 * @param longName message long name
		 * @return self
		 */
		public Builder setLongName(String longName) {
			_builder.setLongname(longName);
			return this;
		}

		/**
		 * name getter
		 * @return message long name
		 */
		public String getName() {
			return _builder.getName();
		}

		/**
		 * Sets message name
		 * @param name message long name
		 * @return self
		 */
		public Builder setName(String name) {
			_builder.setName(name);
			return this;
		}

		/**
		 * Type getter
		 * @return message type
		 */
		public WireMessage.Type getType() {
			return _builder.getType();
		}

		/**
		 * Sets message type
		 * @param type message type
		 * @return self
		 */
		public Builder setType(WireMessage.Type type) {
			_builder.setType(type);
			return this;
		}

		/**
		 * Service id getter
		 * @return message service id
		 */
		int getServiceId() {
			return _builder.getServiceId();
		}

		/**
		 * Sets sub service id
		 * @param serviceId subservice id
		 * @return self
		 */
		public Builder setServiceId(int serviceId) {
			_builder.setServiceId(serviceId);
			return this;
		}

		/**
		 * Seq getter
		 * @return seq number
		 */
		int getSeq() {
			return _builder.getSeq();
		}

		/**
		 * Sets seq number
		 * @param seqNum seq number
		 * @return self
		 */
		public Builder setSeq(int seqNum) {
			_builder.setSeq(seqNum);
			return this;
		}

		/**
		 * Adds long field to message
		 * @param tag field tag
		 * @param val field value
		 * @param loc field location
		 * @return self
		 */
		public Builder addField(int tag, long val, UFEFieldLocation loc) {
			UFEField field = UFEField.newBuilder().setTag(tag).setType(ft_int).setIval(val).setLocation(loc).build();
			_builder.addFields(field);
			return this;
		}

		/**
		 * Adds ByteString field to message
		 * @param tag field tag
		 * @param val field value
		 * @param loc field location
		 * @return self
		 */
		public Builder addField(int tag, ByteString val, UFEFieldLocation loc) {
			UFEField field = UFEField.newBuilder().setTag(tag).setType(ft_string).setSval(val).setLocation(loc).build();
			_builder.addFields(field);
			return this;
		}

		/**
		 * Adds String field to message
		 * @param tag field tag
		 * @param val field value
		 * @param loc field location
		 * @return self
		 */
		public Builder addField(int tag, String val, UFEFieldLocation loc) {
			UFEField field = UFEField.newBuilder().setTag(tag).setType(ft_string).setSval(ByteString.copyFromUtf8(val)).setLocation(loc).build();
			_builder.addFields(field);
			return this;
		}

		/**
		 * Adds char field to message
		 * @param tag field tag
		 * @param val field value
		 * @param loc field location
		 * @return self
		 */
		public Builder addField(int tag, char val, UFEFieldLocation loc) {
			ByteString bstr = ByteString.copyFrom(new byte[]{(byte) val});
			return addField(tag, bstr, loc);
		}

		/**
		 * Adds double field to message
		 * @param tag field tag
		 * @param val field value
		 * @param loc field location
		 * @param precision double field precision
		 * @return self
		 */
		public Builder addField(int tag, double val, UFEFieldLocation loc, int precision) {
			UFEField field = UFEField.newBuilder().setTag(tag).setType(ft_double).setFval(val).setLocation(loc).setIval(precision).build();
			_builder.addFields(field);
			return this;
		}

		/**
		 * Adds bool field to message
		 * @param tag field tag
		 * @param val field value
		 * @param loc field location
		 * @return self
		 */
		public Builder addField(int tag, boolean val, UFEFieldLocation loc) {
			UFEField field = UFEField.newBuilder().setTag(tag).setType(ft_bool).setBval(val).setLocation(loc).build();
			_builder.addFields(field);
			return this;
		}

		/**
		 * Adds time field to message
		 * @param tag field tag
		 * @param val field value
		 * @param loc field location
		 * @return self
		 */
		public Builder addField(int tag, Instant val, UFEFieldLocation loc) {
			UFEField field = UFEField.newBuilder().setTag(tag).setType(ft_time).setIval(val.getEpochSecond()*1000000000L+val.getNano()).setLocation(loc).build();
			_builder.addFields(field);
			return this;
		}

		/**
		 * Adds UUID field to message
		 * @param tag field tag
		 * @param val field value
		 * @param loc field location
		 * @return self
		 */
		public Builder addField(int tag, UUID val, UFEFieldLocation loc) {
			ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
			bb.putLong(val.getMostSignificantBits());
			bb.putLong(val.getLeastSignificantBits());
			UFEField field = UFEField.newBuilder().setTag(tag).setType(ft_uuid).setSval(ByteString.copyFrom(bb.array())).setLocation(loc).build();
			_builder.addFields(field);
			return this;
		}

		/**
		 * Adds status field to message
		 * @param tag field tag
		 * @param val field value
		 * @param loc field location
		 * @return self
		 */
		public Builder addField(int tag, Status val, UFEFieldLocation loc) {
			UFEField field = UFEField.newBuilder().setTag(tag).setType(ft_status).setIval(val.status()).setLocation(loc).build();
			_builder.addFields(field);
			return this;
		}

		/**
		 * Adds object field to message
		 * @param tag field tag
		 * @param val field value
		 * @param loc field location
		 * @return self
		 */
		public Builder addField(int tag, Object val, UFEFieldLocation loc) throws UFEedException {
			if (val instanceof String)
				addField(tag, (String)val, loc);
			else if (val instanceof Character)
				addField(tag, (char)val, loc);
			else if (val instanceof Double)
				addField(tag, (double)val, loc);
			else if (val instanceof Long)
				addField(tag, (long)val, loc);
			else if (val instanceof Integer)
				addField(tag, (int)val, loc);
			else if (val instanceof Boolean)
				addField(tag, (boolean)val, loc);
			else if (val instanceof ByteString)
				addField(tag, (ByteString) val, loc);
			else if (val instanceof UUID)
				addField(tag, (UUID) val, loc);
			else if (val instanceof Instant)
				addField(tag, (Instant) val, loc);
			else if (val instanceof Status)
				addField(tag, (Status) val, loc);
			else
				throw new UFEedException(String.format("object type is unsupported, '%s'", val));
			return this;
		}

		/**
		 * Adds field list to message
		 * @param fields field list to add
		 * @return self
		 */
		public Builder addFields(Iterable<UFEField> fields) {
			_builder.addAllFields(fields);
			return this;
		}

		public static class GroupBuilderRef {
			public UFEField.Builder _builder;
		}

		public interface GroupTransformFunc {
			void tr(UFEMessage.Builder builder, UFEField.Builder group);
		}

		/**
		 * Adds group
		 * @param group output param to get group builder ref
		 * @param tr group transformation function
		 * @param loc group location
		 * @return created message builder
		 */
		public Builder addGroup(int tag, GroupBuilderRef group, GroupTransformFunc tr, UFEFieldLocation loc) {
			UFEField.Builder field = UFEField.newBuilder().setTag(tag).setType(ft_msg).setLocation(loc);
			if (tr != null)
				tr.tr(this, field);
			field.setIval(field.getMvalCount());
			_builder.addFields(field);
			group._builder = field;
			return this;
		}

		/**
		 * Adds group item
		 * @param group group to add item to
		 * @return created item builder
		 */
		public Builder addGroupItem(UFEField.Builder group) {
			return new UFEMessage.Builder(group.addMvalBuilder());
		}

		/**
		 * Builds UFEMessage when composing is complete. Message is ummutable agter the call.
		 * @return Immutable composed UFEMessage
		 */
		public UFEMessage build() {
			WireMessage wm = _builder.build();
			return new UFEMessage(wm);
		}

		/**
		 * Prints message content to string
		 * @return printed message content
		 */
		public String print() {
			return printWm(_builder.buildPartial(), 0);
		}

		static public String printWm(WireMessage wm, int depth) {
			String dspacer = CharBuffer.allocate((1 + depth) * 3).toString().replace('\0', ' ');
			String sspacer = "   ";
			StringBuilder sb = new StringBuilder();
			sb .append(CharBuffer.allocate(depth * 3).toString().replace('\0', ' '))
				.append("srvc_id=").append(wm.getServiceId())
				.append(" subsrvc_id=").append(wm.getSubserviceId())
				.append(" type=").append(wm.getType());
			if (!wm.getName().isEmpty())
				sb.append(" msg=").append(wm.getName());
			if (!wm.getLongname().isEmpty())
				sb.append(" (").append(wm.getLongname()).append(')');
			sb.append(" seq=").append(wm.getSeq()).append('\n');
			for(int i = 0; i < wm.getFieldsCount(); ++i) {
				UFEField f = wm.getFields(i);
				sb.append(dspacer).append(f.getTag()).append(" (");
				switch (f.getLocation()) {
					case fl_body   : sb.append("body"); break;
					case fl_header : sb.append("hdr" ); break;
					case fl_trailer: sb.append("trl" ); break;
					case fl_system : sb.append("sys" ); break;
					default        : sb.append("unknown" ); break;
				}
				sb.append("): ");
				switch (f.getType()) {
					case ft_int:
						sb.append(sspacer).append(f.getIval()).append('\n');
						break;
					case ft_char:
						sb.append(sspacer).append(f.getSval().byteAt(0)).append('\n');
						break;
					case ft_double:
						sb.append(sspacer).append(f.getFval()).append(" (").append(f.getIval()).append(")\n");
						break;
					case ft_string:
						sb.append(sspacer).append(f.getSval().toStringUtf8()).append('\n');
						break;
					case ft_bool:
						sb.append(sspacer).append(f.getBval() ? 'Y' : 'N').append('\n');
						break;
					case ft_time:
						sb .append(sspacer)
							.append(Instant.ofEpochMilli(f.getIval()/1000000).plusNanos(f.getIval()%1000000).toString()).append('\n');;
						break;
					case ft_uuid:
						ByteBuffer bb = ByteBuffer.allocate(64);
						f.getSval().copyTo(bb);
						bb.position(0);
						sb .append(sspacer)
							.append(new UUID(bb.getLong(), bb.getLong()).toString()).append('\n');;
						break;
					case ft_status:
						sb.append(sspacer).append("status(").append(f.getIval()).append(')').append('\n');
						break;
					case ft_msg:
						sb.append(sspacer).append(f.getIval()).append(" elements, depth=").append(depth).append(" ... ").append('\n');
						for(int j = 0; j < f.getMvalCount(); ++j) {
							sb.append(printWm(f.getMval(j), depth + 1));
						}
						break;
					default:
						sb.append("Unknown type:").append(f.getType()).append('\n');
						break;
				}
			}
			return sb.toString();
		}
	}

	/**
	 * Creates a message builder. When it's done, call builder.build() that returns UFEMessage.
	 * @param wm WireMessage to copy from or null
	 * @return message builder
	 */
	public static Builder newBuilder(WireMessage wm) {
		return new Builder(wm);
	}

	/**
	 * Constructs privately UFEMessage. USE UFEMessageBuilder to create new messages.
	 * @param wm WireMessage to construct from
	 */
	private UFEMessage(WireMessage wm) {
		_wm = wm;
		remapWireMessage();
	}

	/**
	 * Returns inner WireMessage
	 * @return inner WireMessage
	 */
	public WireMessage getWireMessage() {
		return _wm;
	}

	/**
	 * Returns mapped fields hash map
	 * @return mapped fields hash map
	 */
	public HashMap<Integer, UFEField> getFields() {
		return _fields;
	}

	/**
	 * Returns mapped groups hash map
	 * @return mapped groups hash map
	 */
	public HashMap<Integer, List<UFEMessage>> getGroups() {
		return _groups;
	}

	/**
	 * Finds field by given tag
	 * @param tag tag to find field
	 * @return found field or null
	 */
	public UFEField findField(int tag) {
		return _fields.get(tag);
	}

	/**
	 * Finds field value by given tag
	 * @param tag tag to find field
	 * @return found field or null
	 */
	public Object findFieldValue(int tag) {
		UFEField field = findField(tag);
		if (field == null)
			return null;
		switch (field.getType()) {
			case ft_status:
			case ft_int:
				return field.getIval();
			case ft_char:
				return (char)field.getSval().byteAt(0);
			case ft_double:
				return field.getFval();
			case ft_string:
				return field.getSval().toStringUtf8();
			case ft_bool:
				return field.getBval();
			case ft_time:
				return Instant.ofEpochMilli(field.getIval()/1000000).plusNanos(field.getIval()%1000000);
			case ft_uuid:
				ByteBuffer bb = ByteBuffer.allocate(64);
				field.getSval().copyTo(bb);
				bb.position(0);
				return new UUID(bb.getLong(), bb.getLong());
			case ft_msg:
				return findGroup(tag);
			default:
				return null;
		}
	}

	/**
	 * Finds group by given tag
	 * @param tag tag to find group
	 * @return found group of null
	 */
	public List<UFEMessage> findGroup(int tag) {
		return _groups.get(tag);
	}

	/**
	 * Prints message content to string
	 * @return printed message content
	 */
	public String print() {
		return Builder.printWm(_wm, 0);
	}

	private void remapField(UFEField field) {
		if (field.getType() == ft_msg) {
			if (!_groups.containsKey(field.getTag()))
				_groups.put(field.getTag(), new ArrayList<>());
			List<UFEMessage> msgs = _groups.get(field.getTag());
			for(WireMessage mval: field.getMvalList())
				msgs.add(new UFEMessage(mval));
		}
		_fields.put(field.getTag(), field);
	}

	private void remapWireMessage() {
		for(UFEField field: _wm.getFieldsList())
			remapField(field);
	}
}
