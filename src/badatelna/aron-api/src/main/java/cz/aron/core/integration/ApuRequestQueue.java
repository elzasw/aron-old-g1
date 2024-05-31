package cz.aron.core.integration;

import cz.aron.core.integration.transformagent.TransformAgentClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 17.12.2020.
 */
@Service
@Slf4j
public class ApuRequestQueue {

    @Inject private QueuedApuStore queuedApuStore;
    @Inject private TransformAgentClient transformAgentClient;

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    @Async
    public void add(String apuId, String sourceId) {
        QueuedApu queuedApu = new QueuedApu();
        queuedApu.setApuId(apuId);
        queuedApu.setSourceApuId(sourceId);
        queuedApuStore.create(queuedApu);
    }

    /**
     * Send batch of apu requests to transformagent
     * @return true - some apus were requested, false - nothing to send
     * 
     * Note: call repeatedly until return false
     */
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public boolean sendRequestsBatch() {
        List<QueuedApu> batchToResolve = queuedApuStore.getBatchToResolve();
        if (batchToResolve.isEmpty()) {
            return false;
        }
        List<String> requestedIdsList = new ArrayList<>();
        for (QueuedApu apu : batchToResolve) {
            requestedIdsList.add(apu.getApuId());
            apu.setRequestSent(true);
        }
        transformAgentClient.requestApus(requestedIdsList);
        queuedApuStore.update(batchToResolve);
        return true;
    }
    
    public long removeForApuId(String apuId) {
        return queuedApuStore.removeForApuId(apuId);
    }
}
