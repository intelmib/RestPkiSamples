package sample.controller;

import com.lacunasoftware.restpki.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import sample.Application;
import sample.util.Util;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@Controller
public class XmlSignatureController {

	/**
	 * This action initiates a XML element signature using REST PKI and renders the signature page.
	 * The XML element is recommended in cases which there is a need to sign a specific element of
	 * a XMl.
	 */
	@RequestMapping(value = "/xml-signature/element", method = {RequestMethod.GET})
	public String getElement(
			Model model,
			HttpServletResponse response
	) throws IOException, RestException {

		// Get an instance of the XmlElementSignatureStarter class, responsible for receiving the
		// signature elements and start the signature process.
		XmlElementSignatureStarter signatureStarter = new XmlElementSignatureStarter(Util.getRestPkiClient());

		// Set the path of the XML to be signed, a sample Brazilian fiscal invoice pre-generated.
		signatureStarter.setXml(Util.getSampleNFeFile());

		// Set the ID of the element to be signed.
		signatureStarter.setElementToSIgnId("NFe35141214314050000662550010001084271182362300");

		// Set the signature policy.
		signatureStarter.setSignaturePolicy(SignaturePolicy.NFePadraoNacional);

		// Set the security context to be used to determine trust in the certificate chain. We have
		// encapsulated the security context choice on Util.java.
		signatureStarter.setSecurityContext(Util.getSecurityContextId());

		// Call the startWithWebPki() method, which initiates the signature. This yields the token,
		// a 43-character case-sensitive URL-safe string, which identifies this signature process.
		// We'll use this value to call the signWithRestPki() method on the Web PKI component (see
		// file signature-form.js) and also to complete the signature after the form is submitted
		// (see method post() below). This should not be mistaken with the API access token.
		String token = signatureStarter.startWithWebPki();

		// The token acquired above can only be used for a single signature attempt. In order to
		// retry the signature it is necessary to get a new token. This can be a problem if the user
		// uses the back button of the browser, since the browser might show a cached page that we
		// rendered previously, with a now stale token. To prevent this from happening, we call the
		// method Util.setNoCacheHeaders(), which sets HTTP headers to prevent caching of the page.
		Util.setNoCacheHeaders(response);

		// Render the signature page (templates/xml-full-signature.html).
		model.addAttribute("token", token);
		return "xml-element-signature";
	}

	/**
	 * This action initiates a full XML signature using REST PKI and renders the signature page.
	 * The full XML signature is recommended in cases which there is a need to sign the whole XML
	 * file.
	 */
	@RequestMapping(value = "/xml-signature/full", method = {RequestMethod.GET})
	public String getFull(
			Model model,
			HttpServletResponse response
	) throws IOException, RestException {

		// Get an instance of the FullXmlSignatureStarter class, responsible for receiving the
		// signature elements and start the signature process.
		FullXmlSignatureStarter signatureStarter = new FullXmlSignatureStarter(Util.getRestPkiClient());

		// Set path of the XML to be signed, a sample XML Document.
		signatureStarter.setXml(Util.getSampleXmlFile());

		// Set the location on which to insert the signature node. If the location is not specified,
		// the signature will appended to the root element (which is most usual with enveloped
		// signatures).
		XmlNamespaceManager nsm = new XmlNamespaceManager();
		nsm.addNamespace("ls", "http://www.lacunasoftware.com/sample");
		signatureStarter.setSignatureElementLocation("//ls:signaturePlaceholder", nsm, XmlInsertionOptions.AppendChild);

		// Set the signature policy.
		signatureStarter.setSignaturePolicy(SignaturePolicy.XadesBes);

		// Set the security context to be used to determine trust in the certificate chain. We have encapsulated the
		// security context choice on Util.cs.
		signatureStarter.setSecurityContext(Util.getSecurityContextId());

		// Call the startWithWebPki() method, which initiates the signature. This yields the token, a
		// 43-character case-sensitive URL-safe string, which identifies this signature process.
		// We'll use this value to call the signWithRestPki() method on the Web PKI component
		// (see file static/js/signature-form.js) and also to complete the signature after the form
		// is submitted (see method complete() below). This should not be mistaken with the API
		// access token.
		String token = signatureStarter.startWithWebPki();

		// The token acquired above can only be used for a single signature attempt. In order to
		// retry the signature it is necessary to get a new token. This can be a problem if the user
		// uses the back button of the browser, since the browser might show a cached page that we
		// rendered previously, with a now stale token. To prevent this from happening, we call the
		// method Util.setNoCacheHeaders(), which sets HTTP headers to prevent caching of the page.
		Util.setNoCacheHeaders(response);

		// Render the signature page (templates/Xml-full-signature.html).
		model.addAttribute("token", token);
		return "xml-full-signature";
	}

	/**
	 * This action receives the form submission from the signature page. We'll call REST PKI to
	 * complete the signature.
	 */
	@RequestMapping(value = "/xml-signature", method = {RequestMethod.POST})
	public String post(
			@RequestParam(value = "token", required = true) String token,
			Model model
	) throws IOException, RestException {

		// Get an instance of the XmlSignatureFinisher class, responsible for completing the
		// signature process.
		XmlSignatureFinisher signatureFinisher = new XmlSignatureFinisher(Util.getRestPkiClient());

		// Set the token for this signature (rendered in a hidden input field, see file
		// templates/xml-full-signature.html).
		signatureFinisher.setToken(token);

		// Call the finish() method, which finalizes the signature process and returns the signed
		// XML's bytes.
		byte[] signedXml = signatureFinisher.finish();

		// Get information about the certificate used by the user to sign the file. This method must
		// only be called after calling the finish() method.
		PKCertificate signerCert = signatureFinisher.getCertificateInfo();

		// At this point, you'd typically store the signed PDF on your database. For demonstration
		// purposes, we'll store the XML on a temporary folder and return to the page an identifier
		// that can be used to download it.

		String filename = UUID.randomUUID() + ".xml";
		FileOutputStream output = new FileOutputStream(new File(Application.getTempFolderPath(), filename));
		output.write(signedXml);
		output.close();

		// Render the signature page (templates/xml-signature-info.html)
		model.addAttribute("signerCert", signerCert);
		model.addAttribute("filename", filename);
		return "xml-signature-info";
	}
}
