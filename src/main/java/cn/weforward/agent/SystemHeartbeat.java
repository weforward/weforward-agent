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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.agent.MachineMethods.MachineInfoParam;
import cn.weforward.agent.util.OsMemory;
import cn.weforward.common.sys.VmStat;
import cn.weforward.common.util.StringUtil;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;

/**
 * 系统心跳
 * 
 * @author daibo
 *
 */
public class SystemHeartbeat implements Runnable {
	/** 日志记录器 */
	private static final Logger _Logger = LoggerFactory.getLogger(SystemHeartbeat.class);
	/** 调用方法 */
	private MachineMethods m_Methods;
	/** 名称列表 */
	private List<String> m_Names;
	/** 磁盘 */
	private List<File> m_Dists;
	/** 心跳 */
	private int m_Interval = 3 * 1000;;

	private Thread m_Thread;

	private long m_StartTime;

	public SystemHeartbeat(MachineMethods methods, String name, String path) {
		m_Methods = methods;
		m_Names = Arrays.asList(name.split(";"));
		if (StringUtil.isEmpty(path)) {
			m_Dists = Collections.emptyList();
		} else {
			String[] arr = path.split(";");
			List<File> list = new ArrayList<>();
			for (String v : arr) {
				list.add(new File(v));
			}
			m_Dists = list;
		}
		m_StartTime = System.currentTimeMillis();
		start();
	}

	public long getStartTime() {
		return m_StartTime;
	}

	public long getUpTime() {
		return System.currentTimeMillis() - m_StartTime;
	}

	public void setMeterRegistry(MeterRegistry register) {
		for (String name : m_Names) {
			Tags tags = Tags.of(new ImmutableTag("name", name));
			TimeGauge.builder("weforward.agent.starttime", this, TimeUnit.MILLISECONDS, SystemHeartbeat::getStartTime)
					.tags(tags).register(register);
			TimeGauge.builder("weforward.agent.uptime", this, TimeUnit.MILLISECONDS, SystemHeartbeat::getUpTime)
					.tags(tags).register(register);
			Gauge.builder("weforward.agent.cpunum", this, SystemHeartbeat::getCpuNum).tags(tags).strongReference(true)
					.register(register);
			Gauge.builder("weforward.agent.loadaverage", this, SystemHeartbeat::getLoadAverage).tags(tags)
					.strongReference(true).register(register);
			Gauge.builder("weforward.agent.memoryusable", this, SystemHeartbeat::getMemoryUsable).tags(tags)
					.strongReference(true).register(register);
			Gauge.builder("weforward.agent.memoryused", this, SystemHeartbeat::getMemoryUsed).tags(tags)
					.strongReference(true).register(register);
			Gauge.builder("weforward.agent.memorytotal", this, SystemHeartbeat::getMemoryTotal).tags(tags)
					.strongReference(true).register(register);
			for (File file : getDists()) {
				Tags filetags = Tags.concat(tags, Tags.of((new ImmutableTag("path", file.getAbsolutePath()))));
				Gauge.builder("weforward.agent.distusable", file, File::getUsableSpace).tags(filetags)
						.strongReference(true).register(register);
				LongSupplier s = new LongSupplier() {

					@Override
					public long getAsLong() {
						return file.getTotalSpace() - file.getUsableSpace();
					}
				};
				Gauge.builder("weforward.agent.distused", s, LongSupplier::getAsLong).tags(filetags)
						.strongReference(true).register(register);
				Gauge.builder("weforward.agent.disttotal", file, File::getTotalSpace).tags(filetags)
						.strongReference(true).register(register);
			}
		}
	}

	public void setInterval(int interval) {
		m_Interval = interval;
	}

	public int getCpuNum() {
		return VmStat._cpus;
	}

	public int getLoadAverage() {
		return VmStat.getLoadAverage();
	}

	/** 可用内存 */
	public long getMemoryUsable() {
		return OsMemory.getCurrentUsable();
	}

	/** 已用内存 */
	public long getMemoryUsed() {
		return getMemoryTotal() - getMemoryUsable();
	}

	/** 总内存 */
	public long getMemoryTotal() {
		return OsMemory.getCurrentMax();
	}

	public List<String> getNames() {
		return m_Names;
	}

	public List<File> getDists() {
		return m_Dists;
	}

	public synchronized void start() {
		if (null == m_Thread) {
			m_Thread = new Thread(this, "systemhb");
			m_Thread.start();
		}
	}

	public void stop() {
		m_Thread = null;
	}

	@Override
	public void run() {
		while (null != m_Thread && !m_Thread.isInterrupted()) {
			synchronized (this) {
				try {
					this.wait(m_Interval);
				} catch (InterruptedException e) {
					break;
				}
			}
			try {
				hd();
			} catch (Throwable e) {
				_Logger.error("忽略心跳异常", e);
			}

		}
	}

	private void hd() {
		MachineInfo info = new MachineInfo();
		VmStat.refresh();
		OsMemory.refresh();
		info.setCpuNum(VmStat._cpus);
		info.setLoadAverage(VmStat.getLoadAverage());
		info.setMemoryUsable(getMemoryUsable());
		info.setMemoryTotal(getMemoryTotal());
		List<MachineInfo.Dist> dists = new ArrayList<>();
		for (File f : getDists()) {
			MachineInfo.Dist d = new MachineInfo.Dist();
			d.setName(f.getAbsolutePath());
			d.setDistUsable(f.getUsableSpace());
			d.setDistTotal(f.getTotalSpace());
			dists.add(d);
		}
		info.setDists(dists);
		for (String name : getNames()) {
			MachineInfoParam param = new MachineInfoParam();
			param.setName(name);
			param.setInfo(info);
			m_Methods.heartbeat(param);
		}
	}

}
