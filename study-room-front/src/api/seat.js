// src/api/seat.js
import request from '../utils/request';

// 获取楼层列表
export const getFloors = () => {
  return request.get('/study-room/floors');
};

// 获取指定楼层的区域列表
export const getAreas = (floorId) => {
  return request.get(`/study-room/floors/${floorId}/areas`);
};

// 获取座位列表（可按楼层/区域/日期过滤）
export const getSeats = (params = {}) => {
  return request.get('/study-room/seats', { params });
};

// 获取座位在某一天的进行中预约时段
export const getSeatReservations = (seatId, date) => {
  return request.get(`/study-room/seats/${seatId}/reservations`, { params: { date } });
};

// 预约座位
export const bookSeat = (data) => {
  return request.post('/study-room/seats/book', data);
};

// 取消预约（可按预约记录ID精确取消）
export const cancelBooking = (seatId, reservationId) => {
  return request.delete(`/study-room/seats/${seatId}/cancel`, { params: { reservationId } });
};

// 获取当前用户的预约记录（含座位、楼层、区域信息）
export const getMyReservations = () => {
  return request.get('/study-room/my-reservations');
};

// 签到（待签到 -> 使用中）
export const checkIn = (reservationId) => {
  return request.post(`/study-room/check-in?reservationId=${reservationId}`);
};

// 签退（使用中 -> 已完成）
export const checkOut = (reservationId) => {
  return request.post(`/study-room/check-out?reservationId=${reservationId}`);
};

// 删除预约记录（仅已完成/违约/已取消等历史记录）
export const deleteReservation = (reservationId) => {
  return request.delete(`/study-room/reservations/${reservationId}`);
};

