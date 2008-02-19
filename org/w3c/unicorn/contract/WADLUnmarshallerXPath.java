// $Id: WADLUnmarshallerXPath.java,v 1.2 2008-02-19 12:51:16 dtea Exp $
// Author: Jean-Guilhem Rouel
// (c) COPYRIGHT MIT, ERCIM and Keio, 2006.
// Please first read the full copyright statement in file COPYRIGHT.html
package org.w3c.unicorn.contract;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.unicorn.util.LocalizedString;
import org.xml.sax.SAXException;

/**
 * WADLUnmarshallerXPath<br />
 * Created: May 22, 2006 6:01:14 PM<br />
 * @author Jean-Guilhem ROUEL
 */
public class WADLUnmarshallerXPath implements WADLUnmarshaller {

	private static final Log logger = LogFactory.getLog("org.w3c.unicorn.contract");

	private static NamespaceContext aNamespaceContext;

	private List<CallMethod> listOfCallMethod = new ArrayList<CallMethod>();

	private DocumentBuilderFactory aDocumentBuilderFactory;
	private DocumentBuilder aDocumentBuilder;
	private Document aDocument;
	private XPath aXPath;
	
	
	/**
	 * Description of a observer to complete with information from a RDF file.
	 */
	//private ObserverDescription aObserverDescription = null;

	private String sID = new String();
	private LocalizedString aLocalizedStringName = new LocalizedString();
	private LocalizedString aLocalizedStringDescription = new LocalizedString();
	private LocalizedString aLocalizedStringHelpLocation = new LocalizedString();
	private LocalizedString aLocalizedStringProvider = new LocalizedString();
	private List<MimeType> listOfMimeType = new ArrayList<MimeType>();
	
	//name of parameter lang if observer has one
	private String nameOfLangParameter = null;
	
	/**
	 * Map of different input method handle by the observer.
	 */
	private Map<EnumInputMethod, InputMethod> mapOfInputMethod = new LinkedHashMap<EnumInputMethod, InputMethod>();
	

	public WADLUnmarshallerXPath () throws ParserConfigurationException {
		WADLUnmarshallerXPath.logger.trace("Constructor");

		this.aDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
		this.aDocumentBuilder = this.aDocumentBuilderFactory.newDocumentBuilder();
		this.aXPath = XPathFactory.newInstance().newXPath();

		this.aXPath.setNamespaceContext(WADLUnmarshallerXPath.aNamespaceContext);
	}

	public void addURL (final URL aURL) throws SAXException, IOException {
		WADLUnmarshallerXPath.logger.trace("addURL");
		if (WADLUnmarshallerXPath.logger.isDebugEnabled()) {
			WADLUnmarshallerXPath.logger.debug("URL : " + aURL + ".");
		}
		this.aDocument = this.aDocumentBuilder.parse(aURL.openStream());
	}

	
	
	
	
	/* (non-Javadoc)
	 * @see org.w3c.unicorn.contract.WADLUnmarshaller#unmarshal(java.net.URL)
	 */
	public void unmarshal() throws XPathExpressionException,
			ParserConfigurationException, SAXException, IOException, MimeTypeParseException {
		WADLUnmarshallerXPath.logger.trace("unmarshal");
		this.parseDocsHeader();
		this.parseMethods();
	}

	private void parseDocsHeader() throws XPathExpressionException, MimeTypeParseException {
		final Node aNodeResource = this.aDocument.getElementsByTagName("resources").item(0);
		XPathExpression aXPathExpression = this.aXPath.compile("//resource/doc");
		NodeList aNodeListResult = (NodeList) aXPathExpression.evaluate(aNodeResource,XPathConstants.NODESET);
		for (int i=0; i<aNodeListResult.getLength(); i++) {
			Node nodeDoc = aNodeListResult.item(i);
			
			String vText = nodeDoc.getTextContent();
			
			//parcours les attrb d un doc
			String vTitle = null;
			String vLang = null;
			NamedNodeMap nnm = nodeDoc.getAttributes();
			for (int j=0; j<nnm.getLength(); j++) {
				String attrName = nnm.item(j).getNodeName();
				String attrValue = nnm.item(j).getNodeValue();
				if ("title".equals(attrName)) {
					vTitle = attrValue;
				} else if ("xml:lang".equals(attrName)) {
					vLang = attrValue;
				}
			}
			
			if ("name".equals(vTitle)) {
				aLocalizedStringName.addLocalization(vLang, vText);
			} else if ("description".equals(vTitle)) {
				aLocalizedStringDescription.addLocalization(vLang, vText);
			} else if ("help".equals(vTitle)) {
				aLocalizedStringHelpLocation.addLocalization(vLang, vText);
			} else if ("provider".equals(vTitle)) {
				aLocalizedStringProvider.addLocalization(vLang, vText);
			} else if ("paramLang".equals(vTitle)) {
				nameOfLangParameter=vText;
			} else if ("mimetype".equals(vTitle)) {
				listOfMimeType.add(new MimeType(vText));
			} else if ("reference".equals(vTitle)) {
				sID=vText;
			}
		}
	}
	
	private void parseMethods() throws ParserConfigurationException,
			SAXException, IOException, XPathExpressionException {
		WADLUnmarshallerXPath.logger.trace("parseMethods");

		// base uri
		final Node aNodeResource = this.aDocument.getElementsByTagName("resources").item(0);
		final String sBaseURI = aNodeResource.getAttributes().getNamedItem("base").getNodeValue();

		final NodeList aNodeListMethod = this.aDocument
				.getElementsByTagName("method");

		for (int i = 0; i < aNodeListMethod.getLength(); i++) {
			final Node aNodeMethod = aNodeListMethod.item(i);

			final Map<String, CallParameter> mapOfCallParameter;
			mapOfCallParameter = new LinkedHashMap<String, CallParameter>();

			// URI of the resource (will be appended to the base URI)
			final String sResourceURI;
			sResourceURI = aNodeMethod.getParentNode().getAttributes().getNamedItem("path").getNodeValue();

			// Type : GET/POST and id of the method
			final NamedNodeMap aNamedNodeMapAttribute = aNodeMethod.getAttributes();
			final String sName = aNamedNodeMapAttribute.getNamedItem("name").getNodeValue();
			final boolean bPost = "POST".equals(sName.trim());
			final String sMethodID = aNamedNodeMapAttribute.getNamedItem("id").getNodeValue().trim();

			// Query variables
			XPathExpression aXPathExpression = this.aXPath.compile("//method[@id='" + sMethodID + "']//param");
			NodeList aNodeListResult = (NodeList) aXPathExpression.evaluate(aNodeMethod,XPathConstants.NODESET);
			
			// iterate over param list
			for (int j = 0; j < aNodeListResult.getLength(); j++) {
				final NamedNodeMap aNamedNodeMap = aNodeListResult.item(j).getAttributes();
				final CallParameter aCallParameter = new CallParameter();
				
				// iterate over attributes
				for (int k = 0; k < aNamedNodeMap.getLength(); k++) {
					final Node aNodeCurrentAttribute = aNamedNodeMap.item(k);

					final String sAttributeName = aNodeCurrentAttribute.getNodeName();
					final String sAttributeValue = aNodeCurrentAttribute.getNodeValue();

					if ("name".equals(sAttributeName)) {
						aCallParameter.setName(sAttributeValue);
					} else if ("required".equals(sAttributeName)) {
						aCallParameter.setRequired("true".equals(sAttributeValue));
					} else if ("repeating".equals(sAttributeName)) {
						aCallParameter.setRepeating("true".equals(sAttributeValue));
					} else if ("fixed".equals(sAttributeName)) {
						aCallParameter.setFixed(sAttributeValue);
					} else if ("style".equals(sAttributeName)) {
						aCallParameter.setStyle(sAttributeValue);
					} else if ("id".equals(sAttributeName)) {
						aCallParameter.setID(sAttributeValue);
					} else if ("path".equals(sAttributeName)) {
						aCallParameter.setPath(sAttributeValue);
					} else if ("default".equals(sAttributeName)) {
						aCallParameter.setDefaultValue(sAttributeValue);
					}
				} // iterate over attributes
				
				// read option type
				
				XPathExpression aOptionXPathExpression = this.aXPath.compile("//method[@id='"+sMethodID+"']//request//param[@name='"+aCallParameter.getName()+"']//option");
				NodeList aOptionNodeListResult = (NodeList) aOptionXPathExpression.evaluate(aNodeMethod,XPathConstants.NODESET);
				
				for (int k=0; k < aOptionNodeListResult.getLength(); k++) {
					aCallParameter.addValue(aOptionNodeListResult.item(k).getAttributes().item(0).getNodeValue());
				}
				
				mapOfCallParameter.put(new String(aCallParameter.getName()),aCallParameter);
				
			} // iterate over query_variable list
			
			
			final CallMethod aCallMethod = new CallMethod(new URL(sBaseURI
					+ sResourceURI), bPost, sName, sMethodID,
					mapOfCallParameter);
			this.listOfCallMethod.add(aCallMethod);
			
			
			//remplir mapOfInputMethod
			
			NodeList listChildMethod = aNodeMethod.getChildNodes();
			String sInputMethod=null;
			String sInputParamName=null;
			for (int j = 0; j < listChildMethod.getLength(); j++) {
				Node childMethod = listChildMethod.item(j);
				if ("doc".equals(childMethod.getNodeName())) {
					String firstAttrName = childMethod.getAttributes().item(0).getNodeName();
					if ("title".equals(firstAttrName)) {
						String firstAttrValue = childMethod.getAttributes().item(0).getNodeValue();
						if ("inputMethod".equals(firstAttrValue))
							sInputMethod = childMethod.getTextContent();
						else if ("inputParamName".equals(firstAttrValue))
							sInputParamName = childMethod.getTextContent();
					}
				}
			}
			
			InputMethod aInputMethod = new InputMethod();
			aInputMethod.setCallMethod(aCallMethod);
			aInputMethod.setCallParameter(aCallMethod.getCallParameterByName(sInputParamName));
			aInputMethod.setListOfMimeType(this.listOfMimeType);
			//aInputMethod.setCallParameter(aInputMethod.getCallMethod().getMapOfCallParameter().get(sParameterName));
			if ("URI".equals(sInputMethod)) {
				this.mapOfInputMethod.put(EnumInputMethod.URI, aInputMethod);
			} else if ("UPLOAD".equals(sInputMethod)){
				this.mapOfInputMethod.put(EnumInputMethod.UPLOAD, aInputMethod);
			} else if ("DIRECT".equals(sInputMethod)){
				this.mapOfInputMethod.put(EnumInputMethod.DIRECT, aInputMethod); 
			}
			
			
			
			
			
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/* (non-Javadoc)
	 * @see org.w3c.unicorn.contract.WADLUnmarshaller#getMethods()
	 */
	public List<CallMethod> getListOfCallMethod() {
		return this.listOfCallMethod;
	}

	static {
		WADLUnmarshallerXPath.aNamespaceContext = new NamespaceContext() {
			public String getNamespaceURI(final String sPrefix) {
				if ("xs".equals(sPrefix)) {
					return "http://www.w3.org/2001/XMLSchema";
				}
				else if ("uco".equals(sPrefix)) {
					return "http://www.w3.org/unicorn/observationresponse";
				}
				else {
					return null;
				}
			}

			public String getPrefix (final String sNamespaceURI) {
				if ("http://www.w3.org/2001/XMLSchema"
						.equals(sNamespaceURI)) {
					return "xs";
				}
				else if ("http://www.w3.org/unicorn/observationresponse"
						.equals(sNamespaceURI)) {
					return "uco";
				}
				else {
					return null;
				}
			}

			public Iterator getPrefixes (final String sNamespaceURI) {
				return null;
			}

		};
	}

	public LocalizedString getDescription() {
		return this.aLocalizedStringDescription;
	}

	public LocalizedString getHelpLocation() {
		return this.aLocalizedStringHelpLocation;
	}

	public String getID() {
		return this.sID;
	}

	public Map<EnumInputMethod, InputMethod> getMapOfInputMethod() {
		return this.mapOfInputMethod;
	}

	public LocalizedString getName() {
		return this.aLocalizedStringName;
	}

	public String getNameOfLangParameter() {
		return this.nameOfLangParameter;
	}

	public LocalizedString getProvider() {
		return this.aLocalizedStringProvider;
	}
	
	public static void main (final String[] args) throws Exception {
		final WADLUnmarshaller t = new WADLUnmarshallerXPath();
		t.addURL(new URL("http://localhost/css.wadl"));
		t.unmarshal();
		System.out.println(t.getID());
		
		/*
		for (CallMethod cm : t.getListOfCallMethod()) {
			System.out.println(cm);
			System.out.println("---------------------------------");
		}
		*/
		System.out.println(t.getMapOfInputMethod());
		System.out.println("***************************************");
		for (InputMethod im : t.getMapOfInputMethod().values()) {
			System.out.println(im.getCallParameter());
			System.out.println("---------------------------------");
		}
	}
	
}
