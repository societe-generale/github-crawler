package com.societegenerale.githubcrawler.mocks;


public interface RemoteServiceMock {

    static final int GITHUB_MOCK_PORT=9900;

    boolean start();

    void stop();

    void reset();
}
