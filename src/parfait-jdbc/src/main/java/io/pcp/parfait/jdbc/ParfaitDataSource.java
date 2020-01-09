/*
 * Copyright 2009-2017 Aconex
 *
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.pcp.parfait.jdbc;

import static tec.uom.se.unit.MetricPrefix.MILLI;
import static tec.uom.se.unit.Units.SECOND;
import static tec.uom.se.AbstractUnit.ONE;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import javax.measure.Unit;
import javax.sql.DataSource;

import io.pcp.parfait.timing.ThreadMetric;
import io.pcp.parfait.timing.ThreadValue;
import io.pcp.parfait.timing.ThreadValueMetric;
import com.google.common.collect.ImmutableList;

public class ParfaitDataSource implements DataSource {
	private final DataSource wrapped;
    private final ThreadLocal<AtomicLong> executionCounts = longThreadLocal();
    private final ThreadLocal<AtomicLong> executionTimes = longThreadLocal();
    private final ThreadMetric counterMetric;  
    private final ThreadMetric timeMetric;  

	public ParfaitDataSource(DataSource wrapped) {
		this.wrapped = wrapped;
		this.counterMetric = newThreadMetric("Database call count", ONE, "db.count",
                "Number of database calls made during event", executionCounts);
		this.timeMetric = newThreadMetric("Database execution time", MILLI(SECOND), "db.time",
               "Time spent in database calls during event", executionTimes);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return proxyConnection(wrapped.getConnection());
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return proxyConnection(wrapped.getConnection(username, password));
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return wrapped.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		wrapped.setLogWriter(out);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return wrapped.getLoginTimeout();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException("Parfait does not support java.util.logging");
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		wrapped.setLoginTimeout(seconds);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return wrapped.isWrapperFor(iface);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return wrapped.unwrap(iface);
	}

	private Connection proxyConnection(Connection wrapped) {
		return (Connection) Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class[] { Connection.class }, new ParfaitConnectionHandler(wrapped));
	}

	private class ParfaitConnectionHandler implements InvocationHandler {
		private final Connection wrapped;

		public ParfaitConnectionHandler(Connection wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				String methodName = method.getName();
				if ("createStatement".equals(methodName) || "prepareStatement".equals(methodName)
						|| "prepareCall".equals(methodName)) {
					return proxyStatement((Statement) method.invoke(wrapped, args));
				}
				return method.invoke(wrapped, args);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private Object proxyStatement(Statement statement) {
			return Proxy.newProxyInstance(this.getClass().getClassLoader(), statement.getClass()
					.getInterfaces(), new StatementInvocationHandler(statement));
		}
	}

	private class StatementInvocationHandler implements InvocationHandler {

		private Statement target;

		public StatementInvocationHandler(Statement target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				String methodName = method.getName();
				if ("execute".equals(methodName) || "executeQuery".equals(methodName)
						|| "executeUpdate".equals(methodName) || "executeBatch".equals(methodName)) {
					return logStatementExecution(method, target, args);
				}
				return method.invoke(target, args);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

	}
	
	protected Object logStatementExecution(Method method, Statement target, Object[] args) throws Exception {
	    long start = System.currentTimeMillis();
	    try {
	        return method.invoke(target, args);
	    } finally {
	        executionCounts.get().incrementAndGet();
	        executionTimes.get().addAndGet(System.currentTimeMillis() - start);
	    }
	}
	
    public final ThreadMetric getCounterMetric() {
        return counterMetric;
    }

    public final ThreadMetric getTimeMetric() {
        return timeMetric;
    }

    protected final ThreadMetric newThreadMetric(String name, Unit<?> unit, String counterSuffix,
            String description, final ThreadLocal<AtomicLong> threadLocal) {
        return new ThreadValueMetric(name, unit, counterSuffix, description,
                new ThreadValue.ThreadLocalMap<Number>(threadLocal));
    }

    public Collection<ThreadMetric> getThreadMetrics() {
        return ImmutableList.<ThreadMetric> of(getCounterMetric(), getTimeMetric());
    }

    protected static final ThreadLocal<AtomicLong> longThreadLocal() {
        return new ThreadLocal<AtomicLong>() {
            @Override
            protected AtomicLong initialValue() {
                return new AtomicLong(0);
            }
        };
    }
}
