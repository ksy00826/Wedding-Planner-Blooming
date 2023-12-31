import { useParams, useNavigate, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { useRecoilState } from "recoil";
import { diaryState } from "../../recoil/DiaryStateAtom";
import { useCallback, useEffect, useState } from "react";
import CreateItem from "../../components/Diary/ModalItem";
import { customAxios } from "../../lib/axios";
import { AiOutlineLeft } from "react-icons/ai"
import { BsTrash } from "react-icons/bs"
import { PiPencilLineFill } from "react-icons/pi"

import classes from "./DiaryDetails.module.css"

const DiaryDetails = () => {

  const [diaries, setDiaries] = useRecoilState(diaryState);
  const [loading, setLoading] = useState(true);
  const [diary, setDiary] = useState()

  const navigate = useNavigate();
  const { id } = useParams();
  const [modalIsVisible, setModalIsVisible] = useState(false);
  
  const location = useLocation();
  const edit = location.state?.edit

  const fetchData = useCallback(async () => {
    try {
      const response = await customAxios.get(`diary/${id}`);
      
      if (response.status === 200) {
        setDiary(response.data.result[0]);
      }
    } catch (error) {
      // console.error(error);
    }
    setLoading(false)
  },[]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  if (loading) {
    return <div>로딩중...</div>;
  }

  const pageTransition = {
    initial: { opacity: 0, x: 500 },
    visible: { opacity: 1, x: 0, transition: { duration: 1 } },
  };

  const hideModalHandler = () => {
    setModalIsVisible(false);
  }
  
  const showModalHandler = () => {
    setModalIsVisible(true);
  }

  const handleGoBack = () => {
    navigate(-1);
  };

  const handleDelete = async () => {
    try {
      await customAxios.delete(`diary/${id}`,);
      if (diaries.length === 1) {
        setDiaries([])
      } else {
        setDiaries(diaries.filter((diary) => 
          diary.id !== id
        ));
      }
      navigate('/diary')
    } catch (error) {
      // console.error(error);
    }
  }

  return (
    <motion.div initial="initial" animate="visible" variants={pageTransition} style={{marginTop:"56px"}}>
      {modalIsVisible ? <CreateItem hide={hideModalHandler} visible={modalIsVisible} item={diary}/> :
        <div className={classes.form}>
          <div className={classes.actions}>
            <div className={classes.back}>
              <button onClick={handleGoBack}><AiOutlineLeft/></button>
            </div>
            
            <div className={classes.date}>
              <div>{diary.date}</div>
            </div>

            {edit && <div className={classes.editdel}>
              <button onClick={showModalHandler}><PiPencilLineFill/></button>
              <button onClick={handleDelete}><BsTrash/></button>
            </div>}
          </div>


          <div className={classes.imageContainer}>
            <img
              src={diary.image ? diary.image : "/src/assets/Icon/nopicture.png"}
              alt="preview"
            />
          </div>

          <div className={classes.text}>
            <textarea readOnly className={classes.title}>{diary.title}</textarea>
            <hr />
            <textarea readOnly className={classes.context}>{diary.content}</textarea>
          </div>
        </div>}
    </motion.div>
  );
};

export default DiaryDetails;
