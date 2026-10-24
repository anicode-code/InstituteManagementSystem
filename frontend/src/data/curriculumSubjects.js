import curriculumData from './curriculumSubjects.json';

export default curriculumData;

/**
 * Helper to get curriculum for a given department code (or ID) and semester
 * @param {string} departmentCode - e.g. "CSE", "IT", "ECE", "ME", "CIVIL"
 * @param {number|string} semester - e.g. 1 to 8
 * @returns {{ year: number, compulsory: Array, optional: Array } | null}
 */
export function getCurriculumByDeptAndSemester(departmentCode, semester) {
  if (!departmentCode || !semester) return null;
  const dept = curriculumData.departments.find(
    (d) => d.code.toUpperCase() === departmentCode.toUpperCase()
  );
  if (!dept || !dept.semesters[String(semester)]) {
    return null;
  }
  return dept.semesters[String(semester)];
}
