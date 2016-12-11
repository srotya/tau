package com.srotya.tau.linea.network;

import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.TauEvent;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * IWC or Inter-Worker Communication Handler is the last Handler in the
 * Netty Pipeline for receiving {@link TauEvent}s from other workers.
 * 
 * @author ambud
 */
public class IWCHandler extends ChannelInboundHandlerAdapter {

	private Router router;

	public IWCHandler(Router router) {
		this.router = router;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		Event event = (Event) msg;
		router.directLocalRouteEvent(event.getHeaders().get(Constants.NEXT_PROCESSOR).toString(),
				(Integer) event.getHeaders().get(Constants.FIELD_DESTINATION_TASK_ID), event);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

}