

@ChannelHandler.Sharable
public class ServerHeartbeatHandler extends ChannelInboundHandlerAdapter {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // 心跳丢失计数器
    private int counter;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //什么都不做是因为链接刚连上来，还没有发送数据上来，无法识别设备
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
        // 判断接收到的包类型
        if (message instanceof ByteBuf) {
        	//数据包按小端去读取
          ByteBuf byteBuf = ((ByteBuf) message).order(ByteOrder.LITTLE_ENDIAN);
          //验证报文内容
          //解包
          //处理业务
          //回包也要转小端
        }

        ReferenceCountUtil.release(message);
    }
   
    /**
     * 心跳空闲处理
     * */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof IdleStateEvent) {
            //心跳包丢失
            if (counter >= 3) {
                // 连续丢失3个心跳包 (断开连接)
                ctx.close();
                counter = 0;
            } else {

                counter++;
               
            }
        }else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
