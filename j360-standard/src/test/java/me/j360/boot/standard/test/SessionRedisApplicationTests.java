/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.j360.boot.standard.test;

import java.net.URI;

import me.j360.boot.standard.J360Configuration;
import org.junit.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.ServerPortInfoApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link J360Configuration}.
 *
 * @author Andy Wilkinson
 */
public class SessionRedisApplicationTests {

	@Test
	public void sessionExpiry() throws Exception {

		String port = null;

		try {
			ConfigurableApplicationContext context = new SpringApplicationBuilder()
					.sources(J360Configuration.class)
					.properties("server.port:0")
					.initializers(new ServerPortInfoApplicationContextInitializer())
					.run();
			port = context.getEnvironment().getProperty("local.server.port");
		}
		catch (RuntimeException ex) {
			if (!redisServerRunning(ex)) {
				return;
			}
		}

		URI uri = URI.create("http://localhost:" + port + "/");
		RestTemplate restTemplate = new RestTemplate();

		ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
		String uuid1 = response.getBody();
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Cookie", response.getHeaders().getFirst("Set-Cookie"));

		RequestEntity<Void> request = new RequestEntity<Void>(requestHeaders,
				HttpMethod.GET, uri);

		String uuid2 = restTemplate.exchange(request, String.class).getBody();
		assertThat(uuid1, is(equalTo(uuid2)));

		Thread.sleep(5000);

		String uuid3 = restTemplate.exchange(request, String.class).getBody();
		assertThat(uuid2, is(not(equalTo(uuid3))));
	}

	private boolean redisServerRunning(Throwable ex) {
		if (ex instanceof RedisConnectionFailureException) {
			return false;
		}
		return (ex.getCause() == null || redisServerRunning(ex.getCause()));
	}

}
