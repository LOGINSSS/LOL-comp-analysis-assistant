package com.LOLCAA.service.ingest;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Riot API 双窗口限流器。
 *
 * 同时约束“每秒请求数”和“两分钟请求数”，避免触发 429。
 */
public class DualWindowRateLimiter {

	private final int perSecond;
	private final int perTwoMinutes;
	private final Deque<Long> secondWindow = new ArrayDeque<>();
	private final Deque<Long> twoMinuteWindow = new ArrayDeque<>();

	public DualWindowRateLimiter(int perSecond, int perTwoMinutes) {
		this.perSecond = perSecond;
		this.perTwoMinutes = perTwoMinutes;
	}

	/**
	 * 获取一次请求许可；若超过阈值则阻塞等待。
	 */
	public synchronized void acquire() {
		while (true) {
			long now = System.currentTimeMillis();
			evictOld(now);

			boolean secondLimited = secondWindow.size() >= perSecond;
			boolean twoMinLimited = twoMinuteWindow.size() >= perTwoMinutes;
			if (!secondLimited && !twoMinLimited) {
				secondWindow.addLast(now);
				twoMinuteWindow.addLast(now);
				return;
			}

			long waitMs = 10L;
			if (secondLimited && !secondWindow.isEmpty()) {
				waitMs = Math.max(waitMs, 1000L - (now - secondWindow.peekFirst()));
			}
			if (twoMinLimited && !twoMinuteWindow.isEmpty()) {
				waitMs = Math.max(waitMs, 120000L - (now - twoMinuteWindow.peekFirst()));
			}

			try {
				wait(waitMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while waiting for Riot rate limit", e);
			}
		}
	}

	/**
	 * 清理超出窗口时间的请求时间戳。
	 */
	private void evictOld(long now) {
		while (!secondWindow.isEmpty() && now - secondWindow.peekFirst() >= 1000L) {
			secondWindow.removeFirst();
		}
		while (!twoMinuteWindow.isEmpty() && now - twoMinuteWindow.peekFirst() >= 120000L) {
			twoMinuteWindow.removeFirst();
		}
		notifyAll();
	}
}
