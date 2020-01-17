package com.societegenerale.githubcrawler.mocks;


public interface RemoteServiceMock {

    static final int GITHUB_MOCK_PORT=9900;

    static final int GITLAB_MOCK_PORT=8800;


    boolean start();

    void stop();

    void reset();
}
