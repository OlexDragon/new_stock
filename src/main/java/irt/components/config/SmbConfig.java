package irt.components.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.smb.session.SmbSessionFactory;

import irt.components.services.HttpSerialPortServersKeeper;
import jcifs.DialectVersion;

@Configuration
public class SmbConfig {

	@Value("${irt.host}")
	private String host;

	@Bean
	public SmbSessionFactory smbSessionFactory() {

		SmbSessionFactory smbSession = new SmbSessionFactory();
	    smbSession.setHost(host);
	    smbSession.setPort(445);
	    smbSession.setDomain("");
	    smbSession.setUsername("Alex");
	    smbSession.setPassword("Dragon64");
	    smbSession.setShareAndDir("files");
	    smbSession.setSmbMinVersion(DialectVersion.SMB210);
	    smbSession.setSmbMaxVersion(DialectVersion.SMB311);

	    return smbSession;
	}

	@Bean
	public HttpSerialPortServersKeeper httpSerialPortServersCollector() {
		return new HttpSerialPortServersKeeper();
	}
}
