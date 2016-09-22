package se.hms.argos.afutest.infra.modbustcp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import se.hms.argos.common.owner.utils.OwnerUtils;

@Configuration
public class ModbustcpBeans
{
   @Bean
   public ModbustcpServer modbustcpServer() throws Exception
   {
      ModbustcpConfig config = OwnerUtils.getOrCreate(ModbustcpConfig.class);
      return new ModbustcpServer(config);
   }
}
