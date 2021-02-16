package com.chatroom.server;

import com.chatroom.entity.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ServerEndpoint(value = "/ws")
@Component
public class WebSocketServer {

    private static Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    private static final AtomicInteger onlineCount = new AtomicInteger(0);
    // juc包的线程安全Set，用来存放每个客户端对应的Session对象。
    private static final CopyOnWriteArraySet<Session> sessionSet = new CopyOnWriteArraySet<Session>();

    private static final ConcurrentHashMap<Session, Object> sessionMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("websocket 加载");
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) throws IOException {
        sessionSet.add(session);
        onlineCount.incrementAndGet(); // 在线数加1
        List<String> sessions = sessionSet.stream().map((s) -> (s.getId())).collect(Collectors.toList());
        sessions.remove(session);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("type", "list");
        hashMap.put("content", sessions);
        BroadCastInfo(new ObjectMapper().writeValueAsString(hashMap));
        log.info("有连接加入，当前连接数为：{}", onlineCount.get());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session) {
        sessionSet.remove(session);
        onlineCount.decrementAndGet();
        log.info("有连接关闭，当前连接数为：{}", onlineCount.get());
    }

    /**
     * 出现错误
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误：{}，Session ID： {}",error.getMessage(),session.getId());
        error.printStackTrace();
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message
     * 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        Message msg = new ObjectMapper().readValue(message, Message.class);
        if ("all".equals(msg.getType())) {
            BroadCastInfo(message);
        }
//        if ("".equals(msg.getType())) {
//            SendMessage(message, );
//        }
        log.info("来自客户端的消息：{}", msg);
    }

    /**
     * 发送消息。
     * @param session
     * @param message
     */
    public static void SendMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        }
        catch (IOException e) {
            log.error("发送消息出错：{}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 群发消息
     * @param message
     * @throws IOException
     */
    public static void BroadCastInfo(String message) {
        for (Session session : sessionSet) {
            if(session.isOpen()){
                SendMessage(session, message);
            }
        }
    }

    /**
     * 指定Session发送消息
     * @param sessionId
     * @param message
     * @throws IOException
     */
    public static void SendMessage(String message, String sessionId) throws IOException {
        Session session = null;
        for (Session s : sessionSet) {
            if(s.getId().equals(sessionId)){
                session = s;
                break;
            }
        }
        if(session!=null){
            SendMessage(session, message);
        }
        else{
            log.warn("没有找到你指定ID的会话：{}",sessionId);
        }
    }
}