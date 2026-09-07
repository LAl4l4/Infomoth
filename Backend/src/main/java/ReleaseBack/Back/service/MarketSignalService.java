package ReleaseBack.Back.service;

import org.springframework.stereotype.Service;
import ReleaseBack.Back.exception.BaseException;
import ReleaseBack.Back.mapper.MarketSignalMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketSignalService {
    private final MarketSignalMapper snapshots;

    public String getSignal() {
        // Serve serialized JSON directly without loading history or rebuilding model output.
        String payload = snapshots.findPayload();
        if (payload == null) {
            throw new BaseException(503, "Market signal snapshot is not ready");
        }
        return payload;
    }
}
