package lep

import com.icthh.xm.tmf.ms.communication.web.api.model.CommunicationMessage
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.RequestContextHolder

String messageId = lepContext.inArgs.id
def httpServletRequest = RequestContextHolder.getRequestAttributes()?.getRequest()
def query = httpServletRequest?.getQueryString()
def lang = query?.split('&').find{ it.startsWith("language=") }?.split('=').last()
def messages = [new CommunicationMessage(content: lang, id: messageId)]
return ResponseEntity.ok(messages)
