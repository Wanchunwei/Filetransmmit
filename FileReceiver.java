import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;

public class FileReceiver {
	private static int expecting = -1;
	public static void main(String[] args) throws Exception
	{
		if (args.length != 1) {
			System.err.println("Usage: SimpleUDPReceiver <port>");
			System.exit(-1);
		}

		String fileName = null;
        int len = 0;
		long filesize = 0;
		int port = Integer.parseInt(args[0]);
		DatagramSocket socket = new DatagramSocket(port);
    	System.out.println("coming    ");


    	byte[] tempBuffer = new byte[1000];
  		DatagramPacket pkt = new DatagramPacket(tempBuffer, tempBuffer.length);
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;

  		while(true){

  			socket.receive(pkt);
			byte[] data = pkt.getData();
			ByteBuffer temp = ByteBuffer.wrap(data);
			long chekn = temp.getLong();
			CRC32 crc= new CRC32();
			crc.update(Arrays.copyOfRange(data, 12, pkt.getLength()));
  			if(crc.getValue() == chekn){

			    int seq_number = temp.getInt();
               System.out.println("Received pkt " + seq_number + "current expecting   " + expecting);
		         if(seq_number == -1){
					 fileName = get_fileName(pkt);
					 try{
						 fos = new FileOutputStream(fileName);
						 bos = new BufferedOutputStream(fos);
					 }catch(Exception e){
						 System.out.println(e.toString());
					 }
					 if(seq_number == expecting){
						 expecting++;
					 }

					 System.out.println("Sending ack " + seq_number);
					 SendAck(socket, pkt.getAddress(), pkt.getPort(), seq_number);
				 }
				 else if(seq_number <= expecting){
					 if(seq_number == 0 && seq_number == expecting){
						 String str = get_filesize(pkt);
						 //System.out.println("THE HEX OF FILESIZE is       " + str);
						 filesize = Long.parseLong(str);
						 expecting++;
						  System.out.println("Sending ack " + seq_number);
						 SendAck(socket, pkt.getAddress(), pkt.getPort(), seq_number);
					 }
					 else{
						 if(seq_number == expecting){
  						  //dealing with data packet
                          expecting++;
  						  byte[] content =Arrays.copyOfRange(pkt.getData(),12,pkt.getLength());
						  if(filesize > content.length){
							 bos.write(content, 0, content.length);
							 len = len +  content.length;
							 filesize = filesize - content.length;
						 }else{
							 bos.write(content, 0, (int)filesize);
							 len = len + (int)filesize;
							  filesize = 0;
						  }
  						  bos.flush();

						  System.out.println("Send pck" + seq_number + " yyyyyyy " +  filesize );
						 // System.out.println("Sending ack " + seq_number);
  						  SendAck(socket, pkt.getAddress(), pkt.getPort(), seq_number);
                         }
			              else{
				           System.out.println(seq_number + "  " + expecting);
                            SendAck(socket, pkt.getAddress(), pkt.getPort(), seq_number);
              }
					 }
				 }else{
                    System.out.println("ack -2");
   					SendAck(socket, pkt.getAddress(), pkt.getPort(), -2);
   				}
 				System.out.println("TOTALLY RECEIVED  PKT" +  seq_number + " " + len);
			}else{
                 //System.out.println(" Checksum incorrect ");
  				SendAck(socket, pkt.getAddress(), pkt.getPort(), -2);
  			}

    }
    }



    public static void SendAck(DatagramSocket socket, InetAddress host, int port, int ack) throws IOException{
      	ByteBuffer bb1 = ByteBuffer.allocate(12);
      	ByteBuffer sequenceNumber = ByteBuffer.allocate(4);
      	sequenceNumber.putInt(ack);
      	bb1.putLong(computeChecksum(sequenceNumber.array()));
      	bb1.putInt(ack);


      	byte[] sendData = bb1.array();
      	DatagramPacket ackPkt = new DatagramPacket(sendData, sendData.length, host, port);
      	socket.send(ackPkt);
		System.out.println("expect " + ack);
    }

	public static long computeChecksum(byte[] data) {
	      CRC32 crc = new CRC32();
	      crc.update(data);
	      return crc.getValue();

	    }

    public static byte[] createHeader(int seq_number, long checksum){
    	 ByteBuffer buffer  = ByteBuffer.allocate(12);
    	 buffer.putInt(seq_number);
    	 buffer.putLong(checksum);
    	 return buffer.array();
    }

	public static String get_filesize(DatagramPacket packet)
    {
        byte[] d = packet.getData();
        byte[] d0 = Arrays.copyOfRange(d, 12, packet.getLength());
        String s = new String(d0);
		System.out.println("Now the String is " + s);
        return s;

    }
    public static String get_fileName(DatagramPacket packet)
    {
        byte[] f = packet.getData();
        byte[] f1 = Arrays.copyOfRange(f, 12, packet.getLength());
        String fileName = new String(f1, 0, f1.length);
        return fileName;

    }


}
