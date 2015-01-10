package de.haendel.jsidplay2;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;

public abstract class DirectoryRequest extends LongRunningRequest<List<String>> {

	public DirectoryRequest(String appName, Connection conn, String url) {
		super(appName, conn, url);
	}

	@Override
	protected List<String> getResult(HttpEntity httpEntity)
			throws IllegalStateException, IOException {
		return receiveList(httpEntity);
	}

	@Override
	protected abstract void onPostExecute(List<String> result);
}
