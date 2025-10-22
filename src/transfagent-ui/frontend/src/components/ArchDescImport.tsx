import { useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { toast } from "react-toastify";

import styled from "styled-components";

import {
    CancelButton,
    ConfirmButton,
    StyledInput,
} from "jareco/dist/common-ui-elements";
import { ModalBase } from "jareco/dist/modal/ModalBase";

import { Api } from "../api";

interface Props {
    onClose: () => void;
}

export const FlexBox = styled.div`
    display: flex;
`;

export const ModalControls = styled(FlexBox)`
    justify-content: flex-end;
`;

export const ArchDescImport = ({onClose}:Props) => {

    const { formatMessage } = useIntl();
    const [submitting, setSubmitting] = useState<boolean>(false);
    const [canSubmit, setCanSubmit] = useState<boolean>(false);
    const [fundVersionId, setFundVersionId] = useState<number>(0);

    const handleClose = () => {
        onClose();        
    };

    const handleSubmit = () => {
        setSubmitting(true);
        if (fundVersionId!=0) {
            Api.transfagent.importArchDesc({fundVersionId})
            .then((result) => {
                toast.success(formatMessage({ id: "archdesc.import.success" }));
                onClose();
                })
                .catch((err) => {
                    console.log("Došlo k chybě", err);
                    onClose();
                });            
        }        
    }

    const handleInputChange = (e: React.FormEvent<HTMLInputElement>) => {
        if (e.currentTarget.value) {
            if (/^\d*$/.test(e.currentTarget.value)) { // allow only digits
                setFundVersionId(Number(e.currentTarget.value));
                setCanSubmit(true);
            } else {
                setCanSubmit(false);
            }
        } else {
            setCanSubmit(false);
        }
    }

    // @ts-ignore  v ModalBase props nejsou children
    return (<ModalBase
                height={100}
                width={350}
                title="Import fondu z Elzyy"                
                onClose={handleClose}
            >
                <div>
                    FundVersionId:
                    <StyledInput 
                            type="number" 
                            onChange={handleInputChange}>                                              
                    </StyledInput>
                </div>
                <div>
                    <br></br>
                </div>
                <div>
                    <ModalControls>
                    <ConfirmButton
                            id="buttonCreate"
                            disabled={submitting||!canSubmit}
                            onClick={handleSubmit}
                        >
                            <FormattedMessage id="archdesc.button.submit" />
                    </ConfirmButton>

                    <CancelButton
                            id="buttonCancel"
                            onClick={handleClose}
                            disabled={submitting}>
                        <FormattedMessage id="archdesc.button.cancel" />
                    </CancelButton>
                    </ModalControls>
                </div>
            </ModalBase>
            );

};