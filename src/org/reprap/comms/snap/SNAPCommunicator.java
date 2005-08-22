package org.reprap.comms.snap;

import org.reprap.Device;
import org.reprap.comms.Address;
import org.reprap.comms.Communicator;
import org.reprap.comms.IncomingContext;
import org.reprap.comms.OutgoingMessage;

public class SNAPCommunicator implements Communicator {

	public SNAPCommunicator(javax.comm.CommPort port, Address localAddress) {
		
	}
	
	public IncomingContext sendMessage(Device device,
			OutgoingMessage messageToSend) {
		
		// TODO actually send message

		IncomingContext replyContext = messageToSend.getReplyContext(this,
				device);
		return replyContext;
	}

}
