/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.jdbc;

import com.google.common.flogger.FluentLogger;
import java.sql.SQLException;
import mil.army.usace.hec.test.database.CwmsDatabaseContainer;
import mil.army.usace.hec.test.database.TeamCityUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.junit.jupiter.Container;


public abstract class CwmsDockerIntegrationTest
{
	private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
	private static final String ORACLE_VERSION = System.getProperty("oracle.version", CwmsDatabaseContainer.ORACLE_19C);
	private static final String IMAGE_VERSION = System.getProperty("cwms.image") != null ? System.getProperty("cwms.image") : "22.1.1-SNAPSHOT";
	private static final String BRANCH = System.getProperty("teamcity.build.branch");
	private static final String VOLUME_NAME = BRANCH != null ? TeamCityUtilities.cleanupBranchName(BRANCH) : "cwms_container_test_db";

	@Container
	private static final CwmsDatabaseContainer INSTANCE = new CwmsDatabaseContainer(ORACLE_VERSION)
			.withSchemaImage(IMAGE_VERSION)
			.withVolumeName(VOLUME_NAME)
			.withOfficeId("NAB")
			.withOfficeEroc("e1")
			.withLogConsumer(o -> logContainerOutput((OutputFrame) o));


	public static String getOracleVersion()
	{
		return ORACLE_VERSION;
	}

	public static String getImageVersion()
	{
		return IMAGE_VERSION;
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
