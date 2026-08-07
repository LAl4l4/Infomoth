import { useState, useEffect } from 'react';
import './Profile.css';
import { useSelector, useDispatch } from 'react-redux';
import { logOut } from '../Variable/login';
import { logout as logoutRequest } from '../API/auth';
import { useNavigate, type NavigateFunction } from 'react-router-dom';
import { getUserProfile } from '../Variable/profile';
import { useForm, type UseFormRegister, type UseFormHandleSubmit, type SubmitHandler } from 'react-hook-form';
import { updateProfile } from '../API/prof';
import type { RootState, AppDispatch } from '../customTypes';


interface ProfileFormValues {
  bio?: string;
  birthday?: string;
  gender?: string;
}

interface ProfilesProps {
  avatar?: string;
  handleLogout: () => void;
  tab: string;
  setTab: (tab: string) => void;
  register: UseFormRegister<ProfileFormValues>;
  handleSubmit: UseFormHandleSubmit<ProfileFormValues>;
  onSubmit: SubmitHandler<ProfileFormValues>;
  isDirty: boolean;
  isSaving: boolean;
}

interface LeftSectionProps {
  tab: string;
  setTab: (tab: string) => void;
}

interface ProfileInfoProps {
  avatar?: string;
  register: UseFormRegister<ProfileFormValues>;
  handleSubmit: UseFormHandleSubmit<ProfileFormValues>;
  onSubmit: SubmitHandler<ProfileFormValues>;
  isDirty: boolean;
  isSaving: boolean;
}

interface EmailPasswordProps {
  handleLogout: () => void;
}

export default function Profile() {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const profile = useSelector((state: RootState) => state.profile.userData);
  const profileError = useSelector((state: RootState) => state.profile.error);
  const [isSaving, setIsSaving] = useState(false);

  //这里拉取Profile
  useEffect(() => {
    dispatch(getUserProfile());
  }, [dispatch]);

  //开发环境会跑两次报错，这个是react18的特性，不是代码错误
  useEffect(() => {
    if (profileError) {
      alert(`获取用户信息失败: ${profileError}`);
    }
  }, [profileError]);

  // 集中管理表单逻辑
  const { register, handleSubmit, formState: { isDirty, dirtyFields }, reset } = useForm<ProfileFormValues>({
    defaultValues: {
      bio: profile.bio || '',
      birthday: profile.birthday || '',
      gender: profile.gender || ''
    },
    values: {
      bio: profile.bio || '',
      birthday: profile.birthday || '',
      gender: profile.gender || ''
    },
    resetOptions: {
      keepDirtyValues: true,
    }
  });

  // 统一的表单提交逻辑
  const onSubmit: SubmitHandler<ProfileFormValues> = async (data) => {
    const patchData: Record<string, unknown> = {};
    Object.keys(dirtyFields).forEach((key) => {
      patchData[key] = (data as Record<string, unknown>)[key];
    });

    try {
      setIsSaving(true);
      const res = await updateProfile(patchData);
      if (res === '保存成功') {
        console.log('保存成功');
        alert('保存成功！');
        reset(data);
        // 关键：提交成功后刷新 Redux 状态
        dispatch(getUserProfile());
        setIsSaving(false);
      } else {
        throw new Error(typeof res === 'string' ? res : '保存失败');
      }
    } catch (error) {
      console.error('保存失败', error);
      alert(`保存失败: ${error instanceof Error ? error.message : String(error)}`);
      setIsSaving(false);
    }
  };

  const [tab, setTab] = useState('info');

  async function handleLogout() {
    await logoutRequest().catch(() => undefined);
    dispatch(logOut());
    navigate('/');
  }

  return (
    <div className="profile-screen">
        <NavigateBar navigate={navigate} />

        <Profiles
          avatar={profile.avatar}
          handleLogout={handleLogout}
          tab={tab}
          setTab={setTab}
          register={register}
          handleSubmit={handleSubmit}
          onSubmit={onSubmit}
          isDirty={isDirty}
          isSaving={isSaving}
        />
    </div>
  );
}



function NavigateBar({ navigate }: { navigate: NavigateFunction }) {
  return (
    <div>
        <div className="profile-header">
            <div className="back-arrow" onClick={()=> navigate(-1)} title="返回">←</div>
            <div className="profile-title-wrap">
            <h1 className="profile-title">个人资料</h1>
            <p className="profile-sub">管理你的账户信息</p>
            </div>
        </div>
    </div>
  );
}

function Profiles(
  { avatar, handleLogout, tab, setTab, register, handleSubmit, onSubmit, isDirty, isSaving }: ProfilesProps
) {
    return (
        <div className='infopart'>
            <LeftSection tab={tab} setTab={setTab} />

      <div className="rightsection">
                {tab === 'info' && (
                    <ProfileInfo
                        avatar={avatar}
                        register={register}
                        handleSubmit={handleSubmit}
                        onSubmit={onSubmit}
                        isDirty={isDirty}
                        isSaving={isSaving}
                    />
                )}

                {tab === "email" && (
                    <EmailPassword
                        handleLogout={handleLogout}
                    />
                )}

                {tab === "others" && <OtherSettings />}
            </div>
        </div>
    );
}

function LeftSection({ tab, setTab }: LeftSectionProps) {
  return (
    <div className='leftsection'>
        <div className="left-section">
            <h2
                className={"card-title" + (tab === 'info' ? " active" : "")}
                onClick={()=>setTab('info')}
            >基本信息</h2>
            <h2
                className={"card-title" + (tab === 'email' ? " active" : "")}
                onClick={()=>setTab('email')}
            >密码/邮箱</h2>
            <h2
                className={"card-title" + (tab === 'others' ? " active" : "")}
                onClick={()=>setTab('others')}
            >其他设置</h2>
        </div>
    </div>
  );
}

function ProfileInfo({ avatar, register, handleSubmit, onSubmit, isDirty, isSaving }: ProfileInfoProps) {
  return (
    <div className='contentsection right-inner'>
        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="card-block">
              <h3 className="section-title">基本信息</h3>
              <div className="field-stack">
                  <div className="field-group">
                      <label className="field-label">Bio</label>
                      <textarea
                        {...register('bio')}
                        placeholder="一句话介绍你自己"
                        className="field-input textarea"
                        rows={3}
                      />
                  </div>

                  <div className="field-group">
                      <label className="field-label">头像链接</label>
                      <input
                        type="text"
                        value={avatar || ''}
                        readOnly={true}
                        className="field-input"
                      />
                  </div>

                  <div className="field-group">
                      <label className="field-label">生日</label>
                      <input
                        type="date"
                        {...register('birthday')}
                        className="field-input"
                      />
                  </div>

                  <div className="field-group">
                      <label className="field-label">性别</label>
                      <select
                        {...register('gender')}
                        className="field-input"
                      >
                        <option value="undisclosed">不公开</option>
                        <option value="male">男</option>
                        <option value="female">女</option>
                        <option value="other">其他</option>
                      </select>
                  </div>

                  <button
                    type="submit"
                    className="save-btn"
                    disabled={!isDirty || isSaving}
                  >
                    {isSaving ? '保存中...' : '保存修改'}
                  </button>
              </div>
          </div>
        </form>
    </div>
  );
}

function EmailPassword({ handleLogout }: EmailPasswordProps) {
  return (
    <div className="contentsection right-inner">
        <div className="card-block">
          <h3 className="section-title">密码 / 邮箱</h3>
          <div className="action-row">
            <button className="primary-btn">修改密码</button>
            <button className="ghost-btn">修改邮箱</button>
          </div>
        </div>

        <div className="card-block">
            <h3 className="section-title">账户</h3>
            <button className="logout-btn" onClick={handleLogout}>退出登录</button>
        </div>
    </div>
  );
}

function OtherSettings() {
  return (
    <div className="contentsection right-inner">
        <div className="card-block">
          <h3 className="section-title">其他设置</h3>
          <div className="decor-text">waitforit</div>
        </div>
    </div>
  );
}
