import java.util.ArrayList;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import org.xml.sax.SAXException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class Directory {

	private ArrayList<Channel> channels;
	
	public Directory() {
		channels = new ArrayList<Channel>();
	}

	// Checks if a channel exists in the directory
	public Channel channelExists(String n) {
		for (int i = 0; i < channels.size(); i++) {
			if (channels.get(i).toString().equals(n)) {
				return channels.get(i);
			}
		}
		return null;
	}
	
	// Add a channel to the directory
	public int addChannel(String n) {
		Channel c = new Channel(n);
		if (channelExists(n) != null) {
			return 0;
		}
		channels.add(new Channel(n));
		return 1;
	}
	
	// Join a channel already in the directory
	public int joinChannel(String n, String p, String m, int id) {
		Channel c = channelExists(n);
		synchronized (c) {
			if (c != null) {
				c.addMember(new Member(m, Integer.parseInt(p), id));
				return 1;
			}
		}

		return 0;
	}
	
	// Leave a channel that you are already a member of
	public int leaveChannel(String n, int id) {
		Channel c = channelExists(n);
		if (c == null) {
			return 0;
		}
		synchronized (c) {
			c.removeMember(id);
		}
		return 0;
	}
	
	// Return a single line string listing the names of all channels seperated
	// by commas
	public String getChannels() {
		String val = "";
		for (int i = 0; i < channels.size(); i++) {
			val += channels.get(i);
			if (i < channels.size() - 1) {
				val += ",";
			}
		}
		return val;
	}
	
	// Return a single line string listing the ip's of all members of a channel,
	// seperated by commas
	public String getMembers(String channel) {
		String val = "";
		Channel c = channelExists(channel);

		if (c == null) {
			return "";
		}

		synchronized (c) {
			for (int i = 0; i < c.getPopulation(); i++) {
				val += c.getMemberByIndex(i).toString();
				if (i < c.getPopulation() - 1) {
					val += ",";
				}
			}
		}

		return val;
	}
	
	// Loads an XML file representing the tracker
	public int loadTracker(String path) {
		try {
			// Build our xml document
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(path);

			// Parse the xml document to build the tracker
			Element root = doc.getDocumentElement();
			String idS = root.getAttribute("counter");
			if (!idS.equals("")) {
				//id = Integer.parseInt(idS);
			}
			NodeList cList = doc.getElementsByTagName("channel");
			for (int i = 0; i < cList.getLength(); i++) {
				Node cNode = cList.item(i);
				Element cElement = (Element) cNode;

				addChannel(cElement.getAttribute("name"));
				Channel c = channelExists(cElement.getAttribute("name"));

				NodeList mList = root.getElementsByTagName("member");
				for (int j = 0; j < mList.getLength(); j++) {
					Node mNode = mList.item(j);
					Element mElement = (Element) mNode;

					String ip = mElement.getAttribute("ip");
					String port = mElement.getAttribute("port");
					int id = Integer.parseInt(mElement.getAttribute("id"));

					c.addMember(new Member(ip, Integer.parseInt(port), id));
				}
			}

		} catch (ParserConfigurationException e) {
			System.out.println(e);
			return 0;
		} catch (UnsupportedEncodingException e) {
			System.out.println(e);
			return 0;
		} catch (SAXException e) {
			System.out.println(e);
			return 0;
		} catch (IOException e) {
			System.out.println(e);
			return 0;
		}

		System.out.println("Tracker loaded");
		return 1;
	}

	// Saves an XML file representing the tracker
	public int saveTracker(String path) {
		try {
			// Build our xml document
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();

			// Add the root element
			Element root = doc.createElement("tracker");
			doc.appendChild(root);

			// Add the id counter
			Attr counterAttr = doc.createAttribute("counter");
			//counterAttr.setValue(Integer.toString(id));
			counterAttr.setValue(Integer.toString(999));
			root.setAttributeNode(counterAttr);

			// Add the channels
			for (int i = 0; i < channels.size(); i++) {
				Element chanElement = doc.createElement("channel");
				Attr attr = doc.createAttribute("name");
				attr.setValue(channels.get(i).toString());
				chanElement.setAttributeNode(attr);
				root.appendChild(chanElement);
				// As well as the members
				for (int j = 0; j < channels.get(i).getPopulation(); j++) {
					Element memberElement = doc.createElement("member");
					Attr idAttr = doc.createAttribute("id");
					idAttr.setValue(String.valueOf(channels.get(i).getMemberByIndex(j).getID()));
					memberElement.setAttributeNode(idAttr);
					Attr ipAttr = doc.createAttribute("ip");
					ipAttr.setValue(channels.get(i).getMemberByIndex(j).getIP());
					memberElement.setAttributeNode(ipAttr);
					Attr portAttr = doc.createAttribute("port");
					portAttr.setValue(Integer.toString(channels.get(i).getMemberByIndex(j).getPort()));
					memberElement.setAttributeNode(portAttr);
					chanElement.appendChild(memberElement);
				}
			}

			// Write the xml to file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(path));
			transformer.transform(source, result);

		} catch (ParserConfigurationException e) {
			System.out.println(e);
			return 0;
		} catch (TransformerException e) {
			System.out.println(e);
			return 0;
		}

		System.out.println("Tracker saved");
		return 1;
	}
}