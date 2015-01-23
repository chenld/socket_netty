package socket.netty.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import socket.netty.msg.AbsMsg;
import socket.netty.msg.MessageID;
import socket.netty.msg.MsgHeader;
import utils.cache.ehcache.main.CacheManagerUtil;
import utils.utils.DateFormateUtil;

/**
 * 消息缓存
 * 
 * @author cuipengfei
 *
 */
public class MsgCache {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = LoggerFactory
			.getLogger(MsgCache.class);

	static MsgCache obj;

	public static MsgCache getInstance() {
		if (obj == null)
			obj = new MsgCache();
		return obj;
	}

	Ehcache cache;

	public MsgCache() {
		this.cache = CacheManagerUtil.getSingletonInstance().getCache("msgcache");
	}

	/**
	 * 存放消息
	 * 
	 * @param msg
	 */
	public void put(AbsMsg msg) {

		if (msg.getHead().getMsg_id() == MessageID.UP_CLOSELINK_INFORM) {
			return;
		}
		String key = getMsgKey(msg);

		MsgObj m = this.get(key);
		if (m == null)
			m = new MsgObj(msg);
		else {
			m.setSendtime(new Date());
			m.setSendedcount(m.getSendedcount() + 1);
		}
		this.put(m);

		if (logger.isDebugEnabled()) {
			logger.debug("消息加入缓存！消息key:" + key);
		}
	}

	public void put(MsgObj msg) {
		AbsMsg m = msg.getMsg();
		Element e = new Element(getMsgKey(m), msg);
		this.cache.put(e);
	}

	public void remove(String key) {
		this.cache.remove(key);
	}

	/**
	 * 根据返回消息 流水号
	 * 
	 * @param key
	 * @return
	 */
	public AbsMsg getMsg(String key) {
		Element e = this.cache.get(key);
		MsgObj m = e == null ? null : (MsgObj) e.getObjectValue();
		return m == null ? null : m.getMsg();

	}

	/**
	 * 根据返回消息 流水号
	 * 
	 * @param key
	 * @return
	 */
	public MsgObj get(String key) {
		Element e = this.cache.get(key);
		return e == null ? null : (MsgObj) e.getObjectValue();

	}

	/**
	 * 
	 * cleanAndgetResendMsg:(清理缓存中的消息，同时将需要重新发送的消息返回).
	 * 
	 * @author sid
	 * @param minInterval
	 *            最小间隔时间，以秒为单位
	 * @param maxCount
	 *            最大重发次数
	 * @param maxTime
	 *            最长缓存时间(小时)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<MsgObj> cleanAndgetResendMsg(int minInterval, int maxCount,
			int maxTime) {
		List<MsgObj> list = new ArrayList<MsgObj>();

		List<String> keys = cache.getKeys();
		Date date = new Date();
		for (String key : keys) {
			MsgObj obj = MsgCache.getInstance().get(key);
			Date endtime = DateFormateUtil.getDateAddHours(obj.getCreatetime(),
					maxTime);

			if (obj.getSendedcount() < maxCount
					&& endtime.getTime() > date.getTime()) {
				long lasttime = obj.getSendtime().getTime();
				long now = System.currentTimeMillis();
				if ((now - lasttime) > (minInterval * 1000)) {
					list.add(obj);
				}
			} else
				MsgCache.getInstance().remove(key);
		}
		return list;

	}


	/**
	 * 根据消息内容生成消息id
	 * @param m
	 * @return
	 */
	public static String getMsgKey(AbsMsg m){
		return m.getHead().getMsg_id()+";"+m.getHead().getMsg_sn();
	}
	/**
	 * 根据消息头生成消息id
	 * @param m
	 * @return
	 */
	public static String getMsgKey(MsgHeader header){
		return header.getMsg_id()+";"+header.getMsg_sn();
	}
}
