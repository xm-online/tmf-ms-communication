package config.testLep

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage
import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationRequestCharacteristic

def message = lepContext.inArgs.message as CommunicationMessage
def value = new CommunicationRequestCharacteristic().name('test').value('ok')
message.addCharacteristicItem(value)


