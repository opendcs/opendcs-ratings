/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/

package org.opendcs.ratings.io.jdbc;

import com.google.common.flogger.FluentLogger;
import java.sql.SQLException;
import mil.army.usace.hec.test.database.CwmsDatabaseContainer;
import mil.army.usace.hec.test.database.CwmsDatabaseContainers;
import mil.army.usace.hec.test.database.TeamCityUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.junit.jupiter.Container;


public abstract class CwmsDockerIntegrationTest
{
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
	private static final String VERSION = "latest-dev";
	private static final String ORACLE_VERSION = System.getProperty("ratings.oracle.version", "ghcr.io/hydrologicengineeringcenter/cwms-database/cwms/database-ready-ora-23.5:" + VERSION);

	@Container
	private static CwmsDatabaseContainer<?> INSTANCE = CwmsDatabaseContainers.createDatabaseContainer(ORACLE_VERSION)
			.withOfficeId("NAB")
			.withOfficeEroc("e1")
			.withLogConsumer(o -> logContainerOutput((OutputFrame) o));
	
	
	

	public static String getOracleVersion()
	{
		return ORACLE_VERSION;
	}

	public static String getImageVersion()
	{
		return VERSION;
	}

	public static CwmsDatabaseContainer<?> getInstance()
	{
		return INSTANCE;
	}

	@BeforeAll
	public static void startContainerInstance() throws SQLException
	{
		INSTANCE.start();
		INSTANCE.connection(Object::toString);
	}

	private static void logContainerOutput(OutputFrame outputFrame)
	{
		OutputFrame.OutputType type = outputFrame.getType();
		switch (type)
		{
			case STDOUT:
				LOGGER.atFinest().log(outputFrame.getUtf8String());
			case STDERR:
				LOGGER.atFine().log(outputFrame.getUtf8String());
			default:
				LOGGER.atFiner().log(outputFrame.getUtf8String());
		}
	}
}
