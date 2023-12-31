import { NavLink, useNavigate } from "react-router-dom";
import { customAxios } from "../../lib/axios";
import { useRecoilState, useRecoilValue } from "recoil";
import { mobileInvitationState } from "../../recoil/MobileInvitationAtom";
import Preview from "../../components/MobileInvitation/Preview";
import classes from "./MobileInvitationDetail.module.css";
import { AiOutlineLeft } from "react-icons/ai";
import { BsTrash } from "react-icons/bs";
import { PiPencilLineFill } from "react-icons/pi";
import { useEffect } from "react";
import { styled } from "styled-components";
import { userState } from "../../recoil/ProfileAtom";

function MobileInvitationDetail() {
  const user = useRecoilValue(userState);
  const [invitation, setInvitation] = useRecoilState(mobileInvitationState);
  const invitationId = invitation.id;
  const navigate = useNavigate();
  const api_key = import.meta.env.VITE_KAKAO_API_KEY;

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await customAxios.get("invitation");
        if (response.data.result[0]) {
          setInvitation(response.data.result[0]);
        } else {
          setInvitation(null);
        }
      } catch (error) {
        // console.log(error);
      }
    };
    fetchData();
  }, [setInvitation]);

  const handleUpdate = () => {
    navigate(`/invitation-create`, {
      state: { id: invitationId },
    });
  };

  const handleDelete = async () => {
    try {
      await customAxios.delete(`invitation/${invitationId}`);
      navigate("/info/mobile-invitation");
    } catch (error) {
      // console.error(error);
    }
  };

  useEffect(() => {
    const createKakaoButton = () => {
      const url = `${import.meta.env.VITE_MOBILE_INVITATION_URL}/${
        invitation.id
      }`;
      if (window.Kakao) {
        // 카카오 스크립트가 로드된 경우 init
        const kakao = window.Kakao;
        if (!kakao.isInitialized()) {
          kakao.init(api_key);
        }
        kakao.Link.createDefaultButton({
          container: "#kakao-link-btn",
          objectType: "feed",
          content: {
            title: "블루밍, 모바일 청첩장",
            description: `${user.name}님의 청첩장`,
            imageUrl: url,
            link: {
              mobileWebUrl: url,
              webUrl: url,
            },
          },
          buttons: [
            {
              title: "웹으로 이동",
              link: {
                mobileWebUrl: url,
                webUrl: url,
              },
            },
          ],
        });
      }
    };
    createKakaoButton();
  }, [invitation]);

  return (
    <div style={{ backgroundColor: "#fff" }}>
      <div className={classes.actions}>
        <div className={classes.back}>
          <button
            onClick={() =>
              navigate("/info/mobile-invitation", {
                state: { navAction: "info" },
              })
            }
          >
            <AiOutlineLeft />
          </button>
        </div>

        <div className={classes.editdel}>
          <button onClick={handleUpdate}>
            <PiPencilLineFill />
          </button>
          <button onClick={handleDelete}>
            <BsTrash />
          </button>
        </div>
      </div>

      <div className={classes.container}>
        <Preview
          positionStyle={{
            "--preview-total-position": "relative",
            "--preview-total-transform": "none",
          }}
          isDetailPage={true}
          showPre={false}
          showCloseButton={false}
          className={classes.modal}
        />
      </div>

      <KakaoBtn id='kakao-link-btn' type='button'>
        <img src='/src/assets/kakaoshare.png' alt='카카오톡으로 공유하기' />
      </KakaoBtn>
    </div>
  );
}

export default MobileInvitationDetail;

const KakaoBtn = styled.button`
  border: none;
  border-radius: 10px;
  margin-bottom: 60px;
  width: 60vw;
  margin-left: 20vw;
  height: calc(60vw * 193 / 1080); // img 비율이 1080:193이라서
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;

  img {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
`;
