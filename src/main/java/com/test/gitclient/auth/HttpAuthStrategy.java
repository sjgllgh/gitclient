package com.test.gitclient.auth;

import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class HttpAuthStrategy implements GitAuthStrategy{
    private String userName;
    private String password;

    public HttpAuthStrategy(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public GitCommand auth(TransportCommand gitCommand) {
        UsernamePasswordCredentialsProvider provider = new UsernamePasswordCredentialsProvider(userName, password);
        return gitCommand.setCredentialsProvider(provider);
    }
}
