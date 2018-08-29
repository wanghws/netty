
public class NettyServer {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);

    private ServerBootstrap bootstrap = new ServerBootstrap();
    private Channel channel;

    private HeartbeatHandler heartbeatHandler = new HeartbeatHandler();

		//停止服务时调用，保证会话都断开
    public boolean destroyed(){

        Future future = bossGroup.shutdownGracefully();
        try {
            future.sync();
            return true;
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
        return false;

    }
    public void start() {


        try {
            bootstrap.group(bossGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {

                            //黑名单
                            IpSubnetFilterRule ipSubnetFilterRule1 = new IpSubnetFilterRule("10.158.0.0",16, IpFilterRuleType.REJECT);

                            //ip拦截器
                            RuleBasedIpFilter ruleBasedIpFilter = new RuleBasedIpFilter(
                                    ipSubnetFilterRule1
                            );

                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(ruleBasedIpFilter);
                            //读空闲为client的心跳频率+3s,因为网络传输会有延迟
                            pipeline.addLast(new IdleStateHandler(18, 0, 0, TimeUnit.SECONDS));
                            pipeline.addLast(serverHeartbeatHandler);
                            //数据类型为小端传输,用C的客户端需要(语言数据精度不同),如果两端都是基于Java的Netty就不需要也不会有问题
                            //为了解决粘包,丢包，需要使用公共报文头来保证内容完整,Netty也有此模式的Encoder和Decoder，但如果客户端不用Netty就没作用，需要自己实现类似逻辑
                            pipeline.addLast( "frameDecoder",new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN,Integer.MAX_VALUE,0,0,0,0,false));
                        }
                    }).option(ChannelOption.SO_BACKLOG, 1024).childOption(ChannelOption.SO_KEEPALIVE, true);//缓冲区大小，当客户端不断发送消息到Server后，需要缓冲区放数据,大小要考虑客户端发送的内容

            channel = bootstrap.bind("127.0.0.1",10240).sync().channel();

						//链接同步
            channel.closeFuture().sync();
            log.warn("Netty Server has started... ");


        } catch (InterruptedException e) {
            log.error(e.getMessage(),e);
        } finally {
            bossGroup.shutdownGracefully();

        }
    }


}
