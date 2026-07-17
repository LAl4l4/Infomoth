package ReleaseBack.Back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ReleaseBack.Back.exception.BaseException;
import ReleaseBack.Back.mapper.SettingsMapper;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private SettingsMapper settingsMapper;

    @InjectMocks
    private SettingsService settingsService;

    @Test
    void getDefaultPageShouldReturnOverviewWhenNoSettingExists() {
        when(settingsMapper.findDefaultPageByUserId(7)).thenReturn(null);

        assertEquals(0, settingsService.getDefaultPage(7));
    }

    @Test
    void updateDefaultPageShouldPersistValidPage() {
        when(settingsMapper.findDefaultPageByUserId(7)).thenReturn(null);

        assertEquals(4, settingsService.updateDefaultPage(7, 4));

        verify(settingsMapper).insertDefaultPage(7, 4);
    }

    @Test
    void updateDefaultPageShouldUpdateExistingSetting() {
        when(settingsMapper.findDefaultPageByUserId(7)).thenReturn(2);

        assertEquals(3, settingsService.updateDefaultPage(7, 3));

        verify(settingsMapper).updateDefaultPage(7, 3);
    }

    @Test
    void updateDefaultPageShouldRejectOutOfRangePage() {
        BaseException exception = assertThrows(
                BaseException.class,
                () -> settingsService.updateDefaultPage(7, 5));

        assertEquals(400, exception.getCode());
        verifyNoInteractions(settingsMapper);
    }
}
