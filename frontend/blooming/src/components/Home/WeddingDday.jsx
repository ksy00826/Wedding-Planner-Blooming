import { useEffect, useState } from "react";
import dayjs from "dayjs";
import { useRecoilState } from "recoil";
import {
  weddingDateState,
  weddingDdayState,
} from "../../recoil/WeddingDdayAtom";
// import "dayjs/locale/ko"; // 한국어 가져오기
import classes from './WeddingDday.module.css'
import { BsFillArrowRightCircleFill } from 'react-icons/bs'
import { Link } from "react-router-dom";

export const WeddingDday = () => {
  const [weddingDate, setWeddingDate] = useRecoilState(weddingDateState);
  const [weddingDday, setWeddingDday] = useRecoilState(weddingDdayState);

  // 나중에 데이터에서 받아오기! 지금은 여기서 설정
  const handleChange = (e) => {
    const newWeddingDate = e.target.value; // input
    setWeddingDate(newWeddingDate);
  };

  useEffect(() => {
    const updateWeddingDday = () => {
      const todayDate = dayjs().format("YYYY-MM-DD");
      const myWeddingDate = dayjs(weddingDate);
      const weddingDiff = myWeddingDate.diff(todayDate, "day");

      if (weddingDiff === 0) {
        setWeddingDday(0);
      } else if (weddingDiff) {
        setWeddingDday(weddingDiff);
      }
    };

    updateWeddingDday();

  }, [weddingDate]);

  // console.log(weddingDate, weddingDday);

  const renderScript = () => {
    if (!weddingDate) {
      return (
        <>
          결혼식을 등록해주세요 &nbsp;
          <Link to="/choose-wedding">
            <BsFillArrowRightCircleFill size={30} className={classes.btn} />
          </Link>
        </>

      )
    }

    const ddayClass =
    weddingDday === 0 || weddingDday > 0
      ? `${classes.dday} ${classes.active}`
      : classes.dday;

  if (weddingDday === 0) {
    return <div className={ddayClass}>D-day</div>;
  } else if (weddingDday > 0) {
    return <div className={ddayClass}>{`D-${weddingDday}`}</div>;
  } else {
    return <div className={ddayClass}>{`D+${Math.abs(weddingDday)}`}</div>;
  }
};

  return (
    <div className={classes.dday}>{renderScript()}</div>
  );
};

export default WeddingDday;
