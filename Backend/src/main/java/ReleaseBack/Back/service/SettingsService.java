package ReleaseBack.Back.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ReleaseBack.Back.exception.BaseException;
import ReleaseBack.Back.mapper.SettingsMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class SettingsService {

    private static final int DEFAULT_PAGE = 0;
    private static final int MAX_PAGE = 4;

    private final SettingsMapper settingsMapper;

    public int getDefaultPage(Integer userId) {
        Integer defaultPage = settingsMapper.findDefaultPageByUserId(userId);
        return defaultPage == null ? DEFAULT_PAGE : defaultPage;
    }

    @Transactional
    public int updateDefaultPage(Integer userId, Integer defaultPage) {
        if (defaultPage == null || defaultPage < DEFAULT_PAGE || defaultPage > MAX_PAGE) {
            throw new BaseException(
                    HttpStatus.BAD_REQUEST.value(),
                    "defaultPage must be between 0 and 4");
        }

        if (settingsMapper.findDefaultPageByUserId(userId) == null) {
            settingsMapper.insertDefaultPage(userId, defaultPage);
        } else {
            settingsMapper.updateDefaultPage(userId, defaultPage);
        }
        return defaultPage;
    }
}
