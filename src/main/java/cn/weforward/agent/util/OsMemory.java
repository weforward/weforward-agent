/**
 * Copyright (c) 2019,2020 honintech
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package cn.weforward.agent.util;

import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.util.Bytes;

/**
 * 操作系统层的内存状态
 * 
 * @author liangyi
 *
 */
public class OsMemory {
	/** 日志记录器 */
	private static final Logger _Logger = LoggerFactory.getLogger(OsMemory.class);

	protected long m_Total;

	protected long m_Free;

	protected long m_Buffers;

	protected long m_Cached;

	protected String m_LinusMemInfo = "/proc/meminfo";

	public final static OsMemory _OS_MEMORY = new OsMemory();

	static public void refresh() {
		String osName = System.getProperty("os.name");
		if (osName.indexOf("Linux") >= 0) {
			_OS_MEMORY.linux();
		} else {
			_Logger.warn("不支持的操作系统:" + osName);
			// TODO 未实现其它系统的方法
		}
	}

	public static long getCurrentUsed() {
		return _OS_MEMORY.getUsed();
	}

	public static long getCurrentUsable() {
		return _OS_MEMORY.getUsable();
	}

	public static long getCurrentMax() {
		return _OS_MEMORY.getMax();
	}

	public long getUsed() {
		return m_Total - getUsable();
	}

	public long getUsable() {
		return m_Free + m_Buffers + m_Cached;
	}

	public long getMax() {
		return m_Total;
	}

	private void reset() {
		m_Total = 0;
		m_Free = 0;
		m_Buffers = 0;
		m_Cached = 0;
	}

	final static byte[] KEY_MEMTOTAL = new byte[] { 'm', 'e', 'm', 't', 'o', 't', 'a', 'l', ':' };
	final static byte[] KEY_MEMFREE = new byte[] { 'm', 'e', 'm', 'f', 'r', 'e', 'e', ':' };
	final static byte[] KEY_BUFFERS = new byte[] { 'b', 'u', 'f', 'f', 'e', 'r', 's', ':' };
	final static byte[] KEY_CACHED = new byte[] { 'c', 'a', 'c', 'h', 'e', 'd', ':' };
	final static int MARK_MEMTOTAL = 0x01;
	final static int MARK_MEMFREE = 0x02;
	final static int MARK_BUFFERS = 0x04;
	final static int MARK_CACHED = 0x08;

	public void linux() {
		byte buf[];
		int len;
		FileInputStream file = null;
		try {
			file = new FileInputStream(m_LinusMemInfo);
			buf = new byte[512];
			len = file.read(buf);
		} catch (IOException e) {
			_Logger.warn("IO异常", e);
			return;
		} finally {
			if (null != file) {
				try {
					file.close();
				} catch (IOException e) {
				}
			}
		}
		reset();
		int offset = 0;
		int marks = 0;
		for (int i = 0; i < len; i++) {
			if ('\n' == buf[i] || '\r' == buf[i]) {
				// 找到回车符或换行符
				if (i > offset + 3) {
					// 找到空格或TAB
					for (int j = offset; j < i; j++) {
						if (' ' == buf[j] || '\t' == buf[j]) {
							if (0 == (MARK_MEMTOTAL & marks) && matchKeyword(KEY_MEMTOTAL, buf, offset, j)) {
								m_Total = parseNumber(buf, j, i);
								marks |= MARK_MEMTOTAL;
							} else if (0 == (MARK_MEMFREE & marks) && matchKeyword(KEY_MEMFREE, buf, offset, j)) {
								m_Free = parseNumber(buf, j, i);
								marks |= MARK_MEMFREE;
							} else if (0 == (MARK_BUFFERS & marks) && matchKeyword(KEY_BUFFERS, buf, offset, j)) {
								m_Buffers = parseNumber(buf, j, i);
								marks |= MARK_BUFFERS;
							} else if (0 == (MARK_CACHED & marks) && matchKeyword(KEY_CACHED, buf, offset, j)) {
								m_Cached = parseNumber(buf, j, i);
								marks |= MARK_CACHED;
							}
							if (marks >= (MARK_MEMTOTAL | MARK_MEMFREE | MARK_BUFFERS | MARK_CACHED)) {
								// 已分析完需要的信息
								return;
							}
							break;
						}
					}
				}
				offset = i + 1;
			}
		}
	}

	private long parseNumber(byte[] bs, int begin, int end) {
		long result = 0;
		int digit;
		while (begin < end) {
			if (' ' != bs[begin] && '\t' != bs[begin]) {
				break;
			}
			++begin;
		}
		while (begin < end && ' ' != bs[begin] && '\t' != bs[begin]) {
			digit = Character.digit(bs[begin++], 10);
			if (digit < 0) {
				// throw NumberFormatException.forInputString(s);
				return -1;
			}
			result *= 10;
			result += digit;
		}
		++begin;
		if (begin + 2 <= end && 'k' == bs[begin] && 'B' == bs[begin + 1]) {
			result *= 1024;
		}
		return result;
	}

	private boolean matchKeyword(byte[] keyword, byte[] bs, int begin, int end) {
		if (keyword.length != end - begin) {
			return false;
		}
		for (int i = 0; i < keyword.length; i++) {
			// if (keyword[i] != bs[i + begin]) {
			if (keyword[i] != Character.toLowerCase(bs[i + begin])) {
				return false;
			}
		}
		return true;
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("{used:");
		Bytes.formatHumanReadable(sb, getUsed());
		sb.append(",usable:");
		Bytes.formatHumanReadable(sb, getUsable());
		sb.append(",max:");
		Bytes.formatHumanReadable(sb, getMax());
		sb.append("}");
		return sb;
	}

	@Override
	public String toString() {
		return toString(new StringBuilder(32)).toString();
	}

}
