/*
   Copyright 2016 Achim Nierbeck

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
package de.nierbeck.exampl.vertx.cluster;

import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import com.hazelcast.core.HazelcastInstance;

import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

@Component(immediate = true, service = {})
public class VertxHazelcastClusterMngrFactory {

    private final static Logger LOGGER = Logger.getLogger("VertxHazelcastClusterMngrFactory");
    private BundleContext bc;
    
    @Reference
    private HazelcastInstance hazelcastInstance;

    private ServiceRegistration<ClusterManager> clusterManagerService;

    @Activate
    public void start(BundleContext bundleContext) {
        this.bc = bundleContext;
        LOGGER.info("Starting VertxHazelcastClusterMngrFactory ... registering ClusterManager as service.");
        ClusterManager mgr = new HazelcastClusterManager(hazelcastInstance);
        clusterManagerService = bc.registerService(ClusterManager.class, mgr, null);
    }
    
    @Deactivate
    public void stop() {
        clusterManagerService.unregister();
    }
    
}
