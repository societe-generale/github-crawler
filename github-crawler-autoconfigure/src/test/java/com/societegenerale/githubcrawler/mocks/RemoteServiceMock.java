package com.societegenerale.githubcrawler.mocks;


public interface RemoteServiceMock {

    public static final int GITHUB_MOCK_PORT=9900;

    public boolean start();
    public void reset();
}
