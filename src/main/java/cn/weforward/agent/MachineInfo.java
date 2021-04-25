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
package cn.weforward.agent;

import java.util.List;

import cn.weforward.protocol.doc.annotation.DocAttribute;

/**
 * 机器信息
 * 
 * @author daibo
 *
 */
public class MachineInfo {
	/** CPU数 */
	protected int m_CpuNum;
	/** 系统负载（0%～m_Cpunum*100%） */
	protected int m_LoadAverage;
	/** 可用内存 */
	protected long m_MemoryUsable;
	/** 总内存 */
	protected long m_MemoryTotal;
	/** 磁盘 */
	protected List<Dist> m_Dists;

	public MachineInfo() {

	}

	/** CPU数 */
	@DocAttribute(description = "CPU数")
	public int getCpuNum() {
		return m_CpuNum;
	}

	/** CPU数 */
	public void setCpuNum(int num) {
		m_CpuNum = num;
	}

	/** 系统负载（0%～CPU数*100%） */
	@DocAttribute(description = "系统负载（0%～CPU数*100%）")
	public int getLoadAverage() {
		return m_LoadAverage;
	}

	/** 系统负载（0%～m_Cpunum*100%） */
	public void setLoadAverage(int average) {
		m_LoadAverage = average;
	}

	/** 可用内存 */
	@DocAttribute(description = "可用内存（单位b）")
	public long getMemoryUsable() {
		return m_MemoryUsable;
	}

	/** 可用内存 */
	public void setMemoryUsable(long v) {
		m_MemoryUsable = v;
	}

	/** 总内存 */
	@DocAttribute(description = "总内存（单位b）")
	public long getMemoryTotal() {
		return m_MemoryTotal;
	}

	/** 总内存 */
	public void setMemoryTotal(long v) {
		m_MemoryTotal = v;
	}

	@DocAttribute(description = "磁盘")
	public List<Dist> getDists() {
		return m_Dists;
	}

	public void setDists(List<Dist> dists) {
		m_Dists = dists;
	}

	public static class Dist {
		/** 名称 */
		protected String m_Name;
		/** 可用磁盘空间 */
		protected long m_DistUsable;
		/** 总磁盘空间 */
		protected long m_DistTotal;

		public Dist() {

		}

		@DocAttribute(description = "名称")
		public String getName() {
			return m_Name;
		}

		public void setName(String name) {
			m_Name = name;
		}

		/** 可用磁盘空间 */
		@DocAttribute(description = "可用磁盘空间（单位b）")
		public long getDistUsable() {
			return m_DistUsable;
		}

		/** 可用磁盘空间 */
		public void setDistUsable(long v) {
			m_DistUsable = v;
		}

		/** 总磁盘空间 */
		@DocAttribute(description = "总磁盘空间（单位b）")
		public long getDistTotal() {
			return m_DistTotal;
		}

		/** 总磁盘空间 */
		public void setDistTotal(long v) {
			m_DistTotal = v;
		}
	}

}
