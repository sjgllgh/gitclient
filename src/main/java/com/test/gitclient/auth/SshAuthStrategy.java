package com.test.gitclient.auth;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.util.FS;

public class SshAuthStrategy implements GitAuthStrategy{

    private String privateKeyPath;

    public SshAuthStrategy(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    @Override
    public GitCommand auth(TransportCommand gitCommand) {
        SshSessionFactory sessionFactory = getSessionFactory();

        return gitCommand.setTransportConfigCallback(transport -> ((SshTransport) transport)
                .setSshSessionFactory(sessionFactory));
    }

    private SshSessionFactory getSessionFactory(){
        return new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch jSch =  super.createDefaultJSch(fs);
                jSch.addIdentity(privateKeyPath);
                return jSch;
            }
        };
    }
}
