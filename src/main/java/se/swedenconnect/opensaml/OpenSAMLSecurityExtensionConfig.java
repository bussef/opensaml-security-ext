/*
 * Copyright 2019 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.swedenconnect.opensaml;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.xmlsec.EncryptionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.swedenconnect.opensaml.xmlsec.ExtendedEncryptionConfiguration;
import se.swedenconnect.opensaml.xmlsec.config.ExtendedDefaultSecurityConfigurationBootstrap;

/**
 * Configuration that extends OpenSAML's encryption support with key agreement algorithms.
 * 
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public class OpenSAMLSecurityExtensionConfig implements OpenSAMLInitializerConfig {
  
  /** Logger instance. */
  private Logger log = LoggerFactory.getLogger(OpenSAMLSecurityExtensionConfig.class);

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return "opensaml-security-extension";
  }

  /**
   * The ECDH support requires that the Bouncy Castle crypto provider is installed. This method ensures this.
   */
  @Override
  public void preInitialize() throws Exception {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      log.info("{}: Crypto provider '{}' is not installed, installing it ...", 
        this.getName(), BouncyCastleProvider.PROVIDER_NAME);
      
      Security.addProvider(new BouncyCastleProvider());
      
      log.info("{}: Crypto provider '{}' was installed", 
        this.getName(), BouncyCastleProvider.PROVIDER_NAME);
    }
  }

  /**
   * Extends OpenSAML's encryption configuration with support for key agreement.
   */
  @Override
  public void postInitialize() throws Exception {
    
    synchronized (ConfigurationService.class) {
      ExtendedEncryptionConfiguration extendedEncryptionConfiguration = null;
      EncryptionConfiguration encryptionConfiguration = ConfigurationService.get(EncryptionConfiguration.class);      
      if (encryptionConfiguration == null) {
        log.warn("{}: OpenSAML does not seem to have installed an EncryptionConfiguration", this.getName());
        extendedEncryptionConfiguration = 
            ExtendedDefaultSecurityConfigurationBootstrap.buildDefaultEncryptionConfiguration();
      }
      else if (ExtendedEncryptionConfiguration.class.isInstance(encryptionConfiguration)) {
        // It seems like the configuration already contains the extensions needed.
        log.debug("{}: ExtendedEncryptionConfiguration already present in OpenSAML configuration", this.getName());
        return;
      }
      else {
        extendedEncryptionConfiguration = ExtendedDefaultSecurityConfigurationBootstrap
            .buildDefaultEncryptionConfiguration(encryptionConfiguration);
      }
            
      ConfigurationService.register(EncryptionConfiguration.class, extendedEncryptionConfiguration);
      log.debug("{}: Extended encryption configuration successfully registered", this.getName());
    }
  }

}