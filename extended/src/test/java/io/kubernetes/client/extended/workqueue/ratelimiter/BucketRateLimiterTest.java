/*
Copyright 2020 The Kubernetes Authors.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package io.kubernetes.client.extended.workqueue.ratelimiter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BucketRateLimiterTest {
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testBucketRateLimiterBasic() {
    RateLimiter<String> rateLimiter = new BucketRateLimiter<>(2, 1, Duration.ofMinutes(10));
    assertThat(rateLimiter.when("one")).isZero();
    assertThat(rateLimiter.when("one")).isZero();

    Duration waitDuration = rateLimiter.when("one");
    Duration expectDuration = Duration.ofMinutes(10);

    Duration diff = waitDuration.minus(expectDuration);
    // waitDuration might be smaller than expect duration because of time is elapsed.
    assertThat(diff.isZero() || (diff.isNegative() && !diff.plusSeconds(1).isNegative())).isTrue();

    waitDuration = rateLimiter.when("one");
    expectDuration = Duration.ofMinutes(20);
    diff = waitDuration.minus(expectDuration);

    assertThat(diff.isZero() || (diff.isNegative() && !diff.plusSeconds(1).isNegative())).isTrue();
  }

  @Test
  public void testBucketRateLimiterTokenAdded() throws InterruptedException {
    RateLimiter<String> rateLimiter = new BucketRateLimiter<>(2, 1, Duration.ofSeconds(2));

    assertThat(rateLimiter.when("one")).isZero();
    assertThat(rateLimiter.when("one")).isZero();

    Duration waitDuration = rateLimiter.when("one");
    assertThat(waitDuration.getSeconds()).isPositive();

    Thread.sleep(4000);

    assertThat(rateLimiter.when("two")).isZero();

    waitDuration = rateLimiter.when("two");
    assertThat(waitDuration.getSeconds()).isPositive();
  }

  @Test
  public void testNegativeCapacity() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("-2 is wrong value for capacity, because capacity should be positive");
    RateLimiter<String> rateLimiter = new BucketRateLimiter<>(-2, 1, Duration.ofSeconds(2));
  }

  @Test
  public void testNegativeTokensGeneratedInPeriod() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("-1 is wrong value for period tokens, because tokens should be positive");
    RateLimiter<String> rateLimiter = new BucketRateLimiter<>(2, -1, Duration.ofSeconds(2));
  }

  @Test
  public void testNegativePeriod() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(
        "-1 is wrong value for period of bandwidth, because period should be positive");
    RateLimiter<String> rateLimiter = new BucketRateLimiter<>(2, 1, Duration.ofNanos(-1));
  }

  @Test
  public void testTokensLargerThanNanos() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(
        "100 token/nanosecond is not permitted refill rate, because highest supported rate is 1 token/nanosecond");
    RateLimiter<String> rateLimiter = new BucketRateLimiter<>(2, 100, Duration.ofNanos(1));
  }
}
