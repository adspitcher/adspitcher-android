package com.adspitcher.net;

import org.json.JSONException;
import org.json.JSONObject;

import com.adspitcher.constants.Constants;
import com.adspitcher.exceptions.ServerException;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

public class NetworkAsyncTask extends AsyncTask<HttpParams, String, byte[]> {

	public static final String TAG = "NetworkSyncTask";
	// Progress Dialog
	private ProgressDialog pDialog;
	private Context reqContext;
	private String progressMsg;
	private Handler responseListener;
	private ConnectivityHandler connHandler;
	private byte[] responseData = null;
	private boolean exceptionRaised;

	public NetworkAsyncTask(Context context, String progressMsg,
			Handler listener) {
		super();
		this.reqContext = context;
		this.progressMsg = progressMsg;
		this.responseListener = listener;
		connHandler = new ConnectivityHandler(context);
		exceptionRaised = false;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		pDialog = new ProgressDialog(reqContext);
		pDialog.setMessage(progressMsg);
		pDialog.setIndeterminate(false);
		pDialog.setCancelable(false);
		pDialog.show();
	}

	@Override
	protected byte[] doInBackground(HttpParams... params) {
		try {
			HttpParams param = params[0];
			Bundle bundle = param.getRequestParamsBundle();
			String requestMethod = bundle.getString(HttpParams.REQUEST_METHOD);
			if (requestMethod.equals(HttpParams.HTTP_GET))
				responseData = connHandler.makeHttpRequest(param);
			else {
				String requestData = bundle.getString(HttpParams.REQUEST_DATA);
				responseData = connHandler.makeHttpPostRequest(param,
						requestData);
			}
		} catch (ServerException exception) {
			exceptionRaised = true;
			responseListener.sendMessage(responseListener.obtainMessage(
					Constants.EXCEPTION, exception));
		}
		return responseData;
	}

	@Override
	protected void onPostExecute(byte[] result) {
		super.onPostExecute(result);
		if (!exceptionRaised) {
			validateJsonData(responseData);
		}
		// dismiss the dialog after getting all products
		pDialog.dismiss();
	}

	/**
	 * Validate Json data
	 */
	public void validateJsonData(byte data[]) {
		Object parsedResult = null;
		// First create string for the given result
		String buffer = new String(data);
		// Now we need to create the json object
		JSONObject jsonObject = null;
		try {
			// Creating Json Object Which Will Hold The Result in Proper Format
			// i.e. Json
			jsonObject = new JSONObject(buffer);
			parsedResult = jsonObject;
		} catch (JSONException exception) {
			parsedResult = exception;
			/*
			 * try { if (jsonObject != null) { if
			 * (!jsonObject.get("error").toString().equals("null")) { try {
			 * JSONObject temp = jsonObject.getJSONObject("error"); parsedResult
			 * = temp.getString("errorMessage"); } catch (Exception ex) {
			 * parsedResult = ex; } } } } catch (JSONException jsonException) {
			 * parsedResult = jsonObject; }
			 */
		}

		if (parsedResult instanceof JSONObject) {
			// successful response
			responseListener.sendMessage(responseListener.obtainMessage(
					Constants.SUCCESSFUL_RESPONSE, parsedResult));
		} else if (parsedResult instanceof String) {
			// handle the error
			responseListener.sendMessage(responseListener.obtainMessage(
					Constants.ERROR, parsedResult));
		} else if (parsedResult instanceof Exception) {
			responseListener.sendMessage(responseListener.obtainMessage(
					Constants.EXCEPTION, parsedResult));
		}
	}

}