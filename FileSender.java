import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;
import java.nio.file.*;

public class FileSender {
	//private static boolean ISEOF = true;
	public static void main(String[] args) throws Exception
	{
		if (args.length != 4) {
			System.err.println("Usage: SimpleUDPSender <host> <port> <source_path> <dest_path>");
			System.exit(-1);
		}
		int dest_portnumber = Integer.parseInt(args[1]);
		InetSocketAddress addr = new InetSocketAddress(args[0], dest_portnumber);
		String file = args[2];

		try{
			SendFile(file, addr, args[3]);
		} catch(Exception e){
			System.out.println(e.toString());
		}

	}



  public static void SendFile(String file, InetSocketAddress addr, String rcvfile) throws Exception{
    DatagramSocket socket = new DatagramSocket();
		DatagramPacket pkt;
        int len = 0;
		int currseq = -1;
	    File f = new File(file);
		long l = f.length();
    //send file name packet
    byte[] rcv = rcvfile.getBytes();

    //checksum of filename
	long rcvchecksum = computeChecksum(rcv);
    System.out.println(rcvchecksum);

    //generate packet
		byte[] namepacket = concatenate(createHeader(currseq, rcvchecksum), rcv);
		pkt = new DatagramPacket(namepacket, namepacket.length, addr);

    System.out.println("sending pkt " + currseq);
	socket.send(pkt);
    //System.out.println("sended\n");
    byte[] tempBuffer = new byte[12];
    DatagramPacket receivedPkt = new DatagramPacket(tempBuffer, tempBuffer.length);
    socket.setSoTimeout(200);
    while(true){
      try{
        socket.receive(receivedPkt);
        System.out.println("Pkt received!");

        ByteBuffer bb3 = ByteBuffer.wrap(receivedPkt.getData());

        long reCheckSum = bb3.getLong();
        int sNumber = bb3.getInt();

	    ByteBuffer seqa = ByteBuffer.allocate(4);
	    seqa.putInt(sNumber);
        long computedCheckSum = computeChecksum(seqa.array());
		//byte[] seqa = new byte[4];
	   // String str3 = "" + sNumber;
	   // seqa = str3.getBytes();
	   // long computedCheckSum = computeChecksum(seqa);
        System.out.println("Receive ack " + sNumber);
        if(computedCheckSum == reCheckSum && sNumber == currseq){
            if(sNumber == -1){
              currseq++;
              break;
            }
          }else{
            socket.send(pkt);
          }
      }catch(SocketTimeoutException e){
        socket.send(pkt);
      }
    }

    String st=String.valueOf(f.length());
	byte[] fileSizeBuffer = st.getBytes();

	System.out.println("The transfer from" + f.length() + "to" + st );

	//checksum of filename
	long filechecksum = computeChecksum(fileSizeBuffer);
	System.out.println(filechecksum);

	//generate packet
	byte[] pct = concatenate(createHeader(currseq, filechecksum), fileSizeBuffer);
	pkt = new DatagramPacket(pct, pct.length, addr);
	System.out.println("sending pkt " + currseq);
	socket.send(pkt);
	//System.out.println("sended\n");
	byte[] tt = new byte[12];
	DatagramPacket receivePkt = new DatagramPacket(tt, tt.length);
	socket.setSoTimeout(200);
	while(true){
	  try{
		socket.receive(receivePkt);
		System.out.println("Pkt received!");
		ByteBuffer buf = ByteBuffer.wrap(receivePkt.getData());
		long CheckSum = buf.getLong();
		int sequence = buf.getInt();

		//byte[] seqb = new byte[4];
	   // String str2 = "" + sequence;
	   // seqb = str2.getBytes();
	   // long computedCheckSum0 = computeChecksum(seqb);
		ByteBuffer seqb = ByteBuffer.allocate(4);
		seqb.putInt(sequence);
	    long computedCheckSum0 = computeChecksum(seqb.array());
		if(computedCheckSum0 == CheckSum){
		System.out.println("Receive ack " + sequence);
		  if(sequence == currseq && sequence == 0){
			  currseq = currseq + 1;
			  break;
			}else{
			socket.send(pkt);
		  }
	  }
	  }catch(SocketTimeoutException e){
		socket.send(pkt);
	  }
	}

	System.out.println("I can pass here");



	FileInputStream fis = new FileInputStream(file);
    BufferedInputStream bis = new BufferedInputStream(fis);
    //BufferedInputStream bis = new BufferedInputStream(FileInputStream(file));
    byte[] databuffer = new byte[988];
    int num;

    while((num = bis.read(databuffer)) != -1){
    		byte[] packet = concatenate(createHeader(currseq, computeChecksum(databuffer)), databuffer);
    		DatagramPacket dpk = new DatagramPacket(packet, packet.length, addr);//524
           //System.out.println("sending1\n");
    		socket.send(dpk);
			//System.out.println("AGAIN CURRENT" + current_seq + " " +  num);
			System.out.println("send " + currseq + " " + num);
              //System.out.println("sended1\n");
            socket.setSoTimeout(100);
        while(true){
          try{
          socket.receive(receivedPkt);
          ByteBuffer bb4 = ByteBuffer.wrap(receivedPkt.getData());
          long CheckSum1 = bb4.getLong();
          int sequence1 = bb4.getInt();
		  ByteBuffer seqn = ByteBuffer.allocate(4);
		  seqn.putInt(sequence1);
		  long computedCheckSum1 = computeChecksum(seqn.array());
	//	  byte[] seqn = new byte[4];
	//	  String str1 = "" + sequence1;
	//	  seqn = str1.getBytes();
	//	 long computedCheckSum1 = computeChecksum(seqn);

          if(computedCheckSum1 == CheckSum1 && sequence1 == currseq){
              currseq = currseq + 1;
			      break;
          }else{
              socket.send(dpk);
          }
          }catch(SocketTimeoutException e){
            socket.send(dpk);
          }
        }
    	}

    }

    private static byte[] concatenate(byte[] buffer1, byte[] buffer2) {
        byte[] returnBuffer = new byte[buffer1.length + buffer2.length];
        System.arraycopy(buffer1, 0, returnBuffer, 0, buffer1.length);
        System.arraycopy(buffer2, 0, returnBuffer, buffer1.length, buffer2.length);
        return returnBuffer;
    }

	private static byte[] createHeader(int seq_number, long checksum){
    	 ByteBuffer buffer = ByteBuffer.allocate(12);
         buffer.putLong(checksum);
    	 buffer.putInt(seq_number);
    	 return buffer.array();
    }

	public static long computeChecksum(byte[] data) {
	  CRC32 crc = new CRC32();
	  crc.update(data);
	  return crc.getValue();

	}
}
