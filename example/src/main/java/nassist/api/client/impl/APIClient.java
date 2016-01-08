package nassist.api.client.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import net.servicestack.client.ConnectionFilter;
import net.servicestack.client.IReturn;
import net.servicestack.client.IReturnVoid;
import net.servicestack.client.JsonServiceClient;
import net.servicestack.client.Log;
import net.servicestack.client.Utils;

@SuppressWarnings({"rawtypes", "unchecked"})
public class APIClient extends JsonServiceClient{

	private static Charset UTF8 = Charset.forName("UTF-8");

	private static final String COOKIES_HEADER = "Set-Cookie";
	private static CookieManager msCookieManager = new CookieManager();

	private IReturn<?> authRequest;
	private Object authResponse;

	public APIClient(String baseUrl) {
		super(baseUrl);
	}

	public APIClient(String baseUrl, IReturn<?> authRequest)
	{
		this(baseUrl);
		this.authRequest = authRequest;
		this.ResponseFilter = new ConnectionFilter() {

			@Override
			public void exec(HttpURLConnection conn) {
				Map<String, List<String>> headerFields = conn.getHeaderFields();
				List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);

				if(cookiesHeader != null)
				{
					for (String cookie : cookiesHeader) 
					{
						msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
					}               
				}
			}
		};

		this.RequestFilter = new ConnectionFilter() {

			@Override
			public void exec(HttpURLConnection conn) {
				if(msCookieManager.getCookieStore().getCookies().size() > 0)
				{
					conn.setRequestProperty("Cookie", join(";",  msCookieManager.getCookieStore().getCookies()));    
				}
			}
		};
	}

	private String join(String delimiter, List<HttpCookie> cookies){
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < cookies.size(); i++) {
			builder.append(cookies.get(i).toString());
			builder.append(delimiter);
		}

		return builder.toString();
	}

	@Override
	public <TResponse> TResponse send(String requestUrl, String httpMethod, byte[] requestBody, String requestType, Object responseClass) {
		HttpURLConnection req = null;
		try {
			req = createRequest(requestUrl, httpMethod, requestBody, requestType);
			Class resClass = responseClass instanceof Class ? (Class)responseClass : null;
			Type resType = responseClass instanceof Type ? (Type)responseClass : null;
			if (resClass == null && resType == null)
				throw new RuntimeException("responseClass '" + responseClass.getClass().getSimpleName() + "' must be a Class or Type");

			int responseCode = req.getResponseCode();
			if (responseCode >= 400){
				boolean success = false;

				if (shouldAuthenticate(req)){
					req.disconnect();
					
					authResponse = post(authRequest);
					
					req = createRequest(requestUrl, httpMethod, requestBody, requestType);
					
					success = req.getResponseCode() < 400;
				}

				if (!success){
					RuntimeException ex = createException(req, responseCode);

					if (ExceptionFilter != null)
						ExceptionFilter.exec(req, ex);

					if (GlobalExceptionFilter != null)
						GlobalExceptionFilter.exec(req, ex);

					throw ex;
				}
			}

			InputStream is = req.getInputStream();

			if (resClass == IReturnVoid.class){
				Utils.readBytesToEnd(is);
				return null;
			}

			if (ResponseFilter != null)
				ResponseFilter.exec(req);
			if (GlobalResponseFilter != null)
				GlobalResponseFilter.exec(req);

			if (resClass == byte[].class)
				return (TResponse)Utils.readBytesToEnd(req);
			if (resClass == String.class)
				return (TResponse)Utils.readToEnd(is, UTF8.name());

			if (Log.isDebugEnabled()) {
				String json = Utils.readToEnd(is, UTF8.name());
				Log.d(json);

				TResponse response = resClass != null ? (TResponse) getGson().fromJson(json, resClass) : (TResponse) getGson().fromJson(json, resType);

						return response;
			}
			else {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				TResponse response = resClass != null ? (TResponse) getGson().fromJson(reader, resClass) : (TResponse) getGson().fromJson(reader, resType);

				reader.close();
				return response;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		finally {
			if (req != null)
				req.disconnect();
		}
	}
	
	public <T> T getUserAuth(){
		if(authResponse == null){
			authResponse = post(authRequest);
		}
		
		return (T) authResponse;
	}
	
    private boolean shouldAuthenticate(HttpURLConnection req){
        try {
            return req.getResponseCode() == 401;
        } catch (IOException e) {
            return false;
        }
    }

}
