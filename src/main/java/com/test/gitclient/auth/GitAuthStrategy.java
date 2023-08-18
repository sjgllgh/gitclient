package com.test.gitclient.auth;

import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;

public interface GitAuthStrategy {
    GitCommand auth(TransportCommand gitCommand);
}
