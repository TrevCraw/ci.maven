/*******************************************************************************
 * (c) Copyright IBM Corporation 2022.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package net.wasdev.wlp.test.dev.it;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DevGenerateFeaturesConflictTest extends BaseDevTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpBeforeClass(null, "../resources/basic-dev-project-umbrella-deps");
    }
 
    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        BaseDevTest.cleanUpAfterClass();
    }

    @Test
    public void featureConfilctTest() throws Exception {
        // Need to have regular feature generation first
        
        // Create conflict with added feature to server configuration
        File srcServerXMLIncludes = new File(tempProj, "/src/main/liberty/config/extraFeatures.xml");
        replaceString("<!-- replace -->", "<feature>webProfile-7.0</feature>", srcServerXMLIncludes);
        Set<String> conflictingFeatureSet = new HashSet<String>(Arrays.asList("servlet-4.0, batch-1.0, webProfile-7.0"));
        Set<String> recommendedFeatureSet = new HashSet<String>(Arrays.asList("servlet-4.0, batch-1.0, webProfile-8.0"));
        final String conflictErrorMsg = String.format(BINARY_SCANNER_CONFLICT_MESSAGE1, conflictingFeatureSet, recommendedFeatureSet);
        boolean msgExists = verifyLogMessageExists(conflictErrorMsg, 100000);
        assertTrue(getLogTail(), msgExists);
        //assertTrue("Could not find the feature conflict message in the process output.\n " + processOutput,
                //processOutput.contains(
                        //String.format(BINARY_SCANNER_CONFLICT_MESSAGE1, conflictingFeatureSet, recommendedFeatureSet)));
        // need to remove webProfile-7.0 from server.xml?
        int conflictErrCount = countOccurrences(conflictErrorMsg, logFile);
        generateFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
        replaceString("<feature>webProfile-7.0</feature>", "<!-- replace -->", srcServerXMLIncludes);
        // verify conflict error did not occur again
        assertTrue(verifyLogMessageExists(conflictErrorMsg, 100000, conflictErrCount));
        // check that generate features count went up
        assertTrue(verifyLogMessageExists(RUNNING_GENERATE_FEATURES, 100000, generateFeaturesCount+1));
    }
}
