package org.reprap.comms.port;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Port {
	public OutputStream getOutputStream() throws IOException;
	public InputStream getInputStream() throws IOException;
	public void close();
}
